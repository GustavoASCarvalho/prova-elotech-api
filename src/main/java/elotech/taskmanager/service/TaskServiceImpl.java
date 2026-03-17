package elotech.taskmanager.service;

import java.util.Locale;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;

import elotech.taskmanager.config.cache.CacheConfig;
import elotech.taskmanager.dto.common.response.PagedResponse;
import elotech.taskmanager.dto.task.request.TaskCreateRequest;
import elotech.taskmanager.dto.task.request.TaskListFiltersRequest;
import elotech.taskmanager.dto.task.request.TaskUpdateRequest;
import elotech.taskmanager.dto.task.response.TaskResponse;
import elotech.taskmanager.dto.task.response.TaskSummaryResponse;
import elotech.taskmanager.entity.Project;
import elotech.taskmanager.entity.Task;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import elotech.taskmanager.exception.BadRequestException;
import elotech.taskmanager.exception.BusinessRuleException;
import elotech.taskmanager.exception.ForbiddenException;
import elotech.taskmanager.exception.NotFoundException;
import elotech.taskmanager.repository.ProjectRepository;
import elotech.taskmanager.repository.TaskRepository;
import elotech.taskmanager.repository.UserProjectRepository;
import elotech.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final String TASK_NOT_FOUND = "Task not found";
    private static final String DEFAULT_SORT_BY_DEADLINE = "deadline";
    private static final long IN_PROGRESS_WIP_LIMIT = 5L;

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    private final UserRepository userRepository;
    private final TaskCacheService taskSummaryCacheService;

    public TaskSummaryResponse getProjectSummary(Long projectId) {
        ensureMembership(projectId);
        Long currentUserId = getCurrentUserId();
        return taskSummaryCacheService.getProjectSummaryCached(projectId, currentUserId);
    }

    public PagedResponse<TaskResponse> searchByText(String text, Integer page, Integer size) {
        if (text == null || text.isBlank()) {
            throw new BadRequestException("Search text is required");
        }
        int currentPage = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0) ? 20 : Math.min(size, 100);
        Long currentUserId = getCurrentUserId();

        Page<TaskResponse> taskPage = taskRepository
                .searchByTextForUser(currentUserId, text.trim(), PageRequest.of(currentPage, pageSize))
                .map(this::toDto);

        return PagedResponse.from(taskPage);
    }

    public PagedResponse<TaskResponse> findAll(TaskListFiltersRequest filters) {
        Long currentUserId = getCurrentUserId();
        int currentPage = (filters.page() == null || filters.page() < 0) ? 0 : filters.page();
        int pageSize = (filters.size() == null || filters.size() <= 0) ? 50 : Math.min(filters.size(), 200);
        Sort sort = buildSort(filters.sortBy(), filters.sortDir());

        Page<TaskResponse> taskPage = taskRepository.findAllByFiltersForUser(
                currentUserId,
                filters.status(),
                filters.priority(),
                filters.assigneeId(),
                filters.deadlineFrom(),
                filters.deadlineTo(),
                PageRequest.of(currentPage, pageSize, sort))
                .map(this::toDto);

        return PagedResponse.from(taskPage);
    }

    public TaskResponse findById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND));
        ensureMembership(task.getProject().getId());
        return toDto(task);
    }

    @CacheEvict(cacheNames = CacheConfig.PROJECT_SUMMARY_CACHE, allEntries = true)
    public TaskResponse create(TaskCreateRequest dto) {
        Task task = new Task();
        applyCreateRequest(dto, task);
        return toDto(taskRepository.save(task));
    }

    @CacheEvict(cacheNames = CacheConfig.PROJECT_SUMMARY_CACHE, allEntries = true)
    public TaskResponse update(Long id, TaskUpdateRequest dto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND));
        ensureMembership(task.getProject().getId());
        applyUpdateRequest(dto, task);
        return toDto(taskRepository.save(task));
    }

    @CacheEvict(cacheNames = CacheConfig.PROJECT_SUMMARY_CACHE, allEntries = true)
    public void delete(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(TASK_NOT_FOUND));
        ensureMembership(task.getProject().getId());
        taskRepository.delete(task);
    }

    private void applyCreateRequest(TaskCreateRequest dto, Task task) {
        Project project = resolveProject(dto.getProjectId());
        User assignee = resolveAssignee(dto.getAssigneeId(), project.getId());

        validateWipLimit(project.getId(), assignee, dto.getStatus(), null);

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());
        task.setProject(project);
        task.setAssignee(assignee);
    }

    private void applyUpdateRequest(TaskUpdateRequest dto, Task task) {
        Project project = resolveProject(dto.getProjectId());
        User assignee = resolveAssignee(dto.getAssigneeId(), project.getId());

        validateStatusTransition(task.getStatus(), dto.getStatus());
        validateCriticalClosePermission(task, dto.getStatus(), dto.getPriority(), project);
        validateWipLimit(project.getId(), assignee, dto.getStatus(), task.getId());

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setStatus(dto.getStatus());
        task.setPriority(dto.getPriority());
        task.setDeadline(dto.getDeadline());
        task.setProject(project);
        task.setAssignee(assignee);
    }

    private void validateStatusTransition(TaskStatusEnum currentStatus, TaskStatusEnum newStatus) {
        if (currentStatus == TaskStatusEnum.DONE && newStatus == TaskStatusEnum.TODO) {
            throw new BusinessRuleException("TASK_INVALID_STATUS_TRANSITION",
                    "A DONE task cannot move back to TODO");
        }
    }

    private void validateCriticalClosePermission(Task currentTask,
            TaskStatusEnum newStatus,
            PriorityEnum newPriority,
            Project project) {
        boolean closingTask = newStatus == TaskStatusEnum.DONE;
        boolean isCriticalTask = currentTask.getPriority() == PriorityEnum.CRITICAL
                || newPriority == PriorityEnum.CRITICAL;

        if (!closingTask || !isCriticalTask) {
            return;
        }

        User currentUser = getCurrentUser();
        boolean isProjectAdmin = project.getOwner() != null
                && project.getOwner().getId().equals(currentUser.getId())
                && currentUser.getRole() == elotech.taskmanager.enums.UserRoleEnum.ADMIN;

        if (!isProjectAdmin) {
            throw new BusinessRuleException("TASK_CRITICAL_CLOSE_REQUIRES_PROJECT_ADMIN",
                    "Only the project ADMIN can close CRITICAL tasks");
        }
    }

    private void validateWipLimit(Long projectId, User assignee, TaskStatusEnum status, Long currentTaskId) {
        if (status != TaskStatusEnum.IN_PROGRESS || assignee == null) {
            return;
        }

        long inProgressCount = currentTaskId == null
                ? taskRepository.countByProject_IdAndAssignee_IdAndStatus(projectId, assignee.getId(),
                        TaskStatusEnum.IN_PROGRESS)
                : taskRepository.countByProject_IdAndAssignee_IdAndStatusAndIdNot(
                        projectId,
                        assignee.getId(),
                        TaskStatusEnum.IN_PROGRESS,
                        currentTaskId);

        if (inProgressCount >= IN_PROGRESS_WIP_LIMIT) {
            throw new BusinessRuleException(
                    "TASK_WIP_LIMIT_EXCEEDED",
                    "WIP limit exceeded: a responsible user can have at most 5 IN_PROGRESS tasks");
        }
    }

    private Project resolveProject(Long projectId) {
        if (projectId == null) {
            throw new BadRequestException("Project is required");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BadRequestException("Project not found"));
        ensureMembership(projectId);
        return project;
    }

    private User resolveAssignee(Long assigneeId, Long projectId) {
        if (assigneeId == null) {
            return null;
        }
        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new BadRequestException("Assignee not found"));
        boolean isMember = userProjectRepository.existsByUser_IdAndProject_Id(assigneeId, projectId);
        if (!isMember) {
            throw new BusinessRuleException("TASK_ASSIGNEE_MUST_BE_PROJECT_MEMBER",
                    "Assignee must be a member of the project");
        }
        return assignee;
    }

    private Sort buildSort(String sortBy, String sortDir) {
        boolean descending = "desc".equalsIgnoreCase(sortDir);
        String normalizedSortBy = sortBy == null ? DEFAULT_SORT_BY_DEADLINE : sortBy.toLowerCase(Locale.ROOT);

        Sort sort;
        switch (normalizedSortBy) {
            case "priority" -> sort = JpaSort.unsafe(
                    "case when priority = 'CRITICAL' then 1 when priority = 'HIGH' then 2 when priority = 'MEDIUM' then 3 when priority = 'LOW' then 4 else 5 end");
            case "createdat" -> sort = Sort.by("createdAt");
            case DEFAULT_SORT_BY_DEADLINE -> sort = Sort.by(DEFAULT_SORT_BY_DEADLINE);
            default -> throw new BadRequestException("Invalid sortBy. Use: priority, createdAt or deadline");
        }

        return descending ? sort.descending() : sort.ascending();
    }

    private void ensureMembership(Long projectId) {
        Long currentUserId = getCurrentUserId();
        boolean isMember = userProjectRepository.existsByUser_IdAndProject_Id(currentUserId, projectId);
        if (!isMember) {
            throw new ForbiddenException("You do not have access to tasks from this project");
        }
    }

    private Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private User getCurrentUser() {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new BadRequestException("No authenticated user in security context");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    private TaskResponse toDto(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .status(task.getStatus())
                .priority(task.getPriority())
                .deadline(task.getDeadline())
                .build();
    }
}
