package elotech.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
import elotech.taskmanager.enums.UserRoleEnum;
import elotech.taskmanager.exception.BadRequestException;
import elotech.taskmanager.exception.BusinessRuleException;
import elotech.taskmanager.exception.ForbiddenException;
import elotech.taskmanager.repository.ProjectRepository;
import elotech.taskmanager.repository.TaskRepository;
import elotech.taskmanager.repository.UserProjectRepository;
import elotech.taskmanager.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserProjectRepository userProjectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TaskCacheService taskSummaryCacheService;

    @InjectMocks
    private TaskService taskService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchByTextShouldThrowWhenTextIsBlank() {
        assertThrows(BadRequestException.class, () -> taskService.searchByText("   ", 0, 10));
    }

    @Test
    void getProjectSummaryShouldReturnCachedDataWhenUserIsMember() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        TaskSummaryResponse cached = TaskSummaryResponse.builder().build();

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);
        when(taskSummaryCacheService.getProjectSummaryCached(10L, 1L)).thenReturn(cached);

        TaskSummaryResponse response = taskService.getProjectSummary(10L);

        assertNotNull(response);
        verify(taskSummaryCacheService).getProjectSummaryCached(10L, 1L);
    }

    @Test
    void getProjectSummaryShouldThrowForbiddenWhenUserIsNotMember() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> taskService.getProjectSummary(10L));
    }

    @Test
    void findByIdShouldThrowForbiddenWhenUserIsNotProjectMember() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);

        Project project = buildProject(77L, null);
        Task task = buildTask(88L, "Task", TaskStatusEnum.TODO, PriorityEnum.LOW, project, null);

        when(taskRepository.findById(88L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 77L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> taskService.findById(88L));
    }

    @Test
    void findByIdShouldReturnTaskWhenUserIsProjectMember() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(77L, null);
        Task task = buildTask(88L, "Task", TaskStatusEnum.TODO, PriorityEnum.LOW, project, null);

        when(taskRepository.findById(88L)).thenReturn(Optional.of(task));
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 77L)).thenReturn(true);

        TaskResponse response = taskService.findById(88L);

        assertEquals(88L, response.getId());
        assertEquals(77L, response.getProjectId());
    }

    @Test
    void findByIdShouldThrowNotFoundWhenTaskDoesNotExist() {
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(elotech.taskmanager.exception.NotFoundException.class, () -> taskService.findById(999L));
    }

    @Test
    void createShouldThrowWhenAssigneeIsNotProjectMember() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        User assignee = buildUser(2L, "assignee@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(userProjectRepository.existsByUser_IdAndProject_Id(2L, 10L)).thenReturn(false);

        TaskCreateRequest request = new TaskCreateRequest(
                "Titulo",
                "Descricao",
                10L,
                2L,
                TaskStatusEnum.TODO,
                PriorityEnum.MEDIUM,
                LocalDateTime.now().plusDays(2));

        assertThrows(BusinessRuleException.class, () -> taskService.create(request));

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void createShouldThrowWhenProjectIdIsNull() {
        TaskCreateRequest request = new TaskCreateRequest(
                "Titulo",
                "Descricao",
                null,
                null,
                TaskStatusEnum.TODO,
                PriorityEnum.MEDIUM,
                LocalDateTime.now().plusDays(2));

        assertThrows(BadRequestException.class, () -> taskService.create(request));
    }

    @Test
    void createShouldThrowWhenProjectDoesNotExist() {
        when(projectRepository.findById(10L)).thenReturn(Optional.empty());

        TaskCreateRequest request = new TaskCreateRequest(
                "Titulo",
                "Descricao",
                10L,
                null,
                TaskStatusEnum.TODO,
                PriorityEnum.MEDIUM,
                LocalDateTime.now().plusDays(2));

        assertThrows(BadRequestException.class, () -> taskService.create(request));
    }

    @Test
    void createShouldThrowWhenCurrentUserIsNotProjectMember() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(false);

        TaskCreateRequest request = new TaskCreateRequest(
                "Titulo",
                "Descricao",
                10L,
                null,
                TaskStatusEnum.TODO,
                PriorityEnum.MEDIUM,
                LocalDateTime.now().plusDays(2));

        assertThrows(ForbiddenException.class, () -> taskService.create(request));
    }

    @Test
    void createShouldThrowWhenAssigneeDoesNotExist() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        TaskCreateRequest request = new TaskCreateRequest(
                "Titulo",
                "Descricao",
                10L,
                2L,
                TaskStatusEnum.TODO,
                PriorityEnum.MEDIUM,
                LocalDateTime.now().plusDays(2));

        assertThrows(BadRequestException.class, () -> taskService.create(request));
    }

    @Test
    void createShouldThrowWhenWipLimitIsExceededOnCreate() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        User assignee = buildUser(2L, "assignee@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(userProjectRepository.existsByUser_IdAndProject_Id(2L, 10L)).thenReturn(true);
        when(taskRepository.countByProject_IdAndAssignee_IdAndStatus(10L, 2L, TaskStatusEnum.IN_PROGRESS))
                .thenReturn(5L);

        TaskCreateRequest request = new TaskCreateRequest(
                "Titulo",
                "Descricao",
                10L,
                2L,
                TaskStatusEnum.IN_PROGRESS,
                PriorityEnum.MEDIUM,
                LocalDateTime.now().plusDays(2));

        assertThrows(BusinessRuleException.class, () -> taskService.create(request));
    }

    @Test
    void createShouldPersistTaskWhenValid() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(501L);
            return saved;
        });

        TaskCreateRequest request = new TaskCreateRequest(
                "Titulo",
                "Descricao",
                10L,
                null,
                TaskStatusEnum.TODO,
                PriorityEnum.MEDIUM,
                LocalDateTime.now().plusDays(2));

        TaskResponse response = taskService.create(request);

        assertEquals(501L, response.getId());
        assertEquals(10L, response.getProjectId());
        assertEquals(null, response.getAssigneeId());
    }

    @Test
    void updateShouldThrowWhenStatusTransitionIsInvalid() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);
        Task task = buildTask(90L, "Task", TaskStatusEnum.DONE, PriorityEnum.LOW, project, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(90L)).thenReturn(Optional.of(task));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Titulo",
                "Descricao",
                10L,
                null,
                TaskStatusEnum.TODO,
                PriorityEnum.LOW,
                LocalDateTime.now().plusDays(1));

        assertThrows(BusinessRuleException.class, () -> taskService.update(90L, request));

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateShouldThrowNotFoundWhenTaskDoesNotExist() {
        when(taskRepository.findById(777L)).thenReturn(Optional.empty());

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Titulo",
                "Descricao",
                10L,
                null,
                TaskStatusEnum.TODO,
                PriorityEnum.LOW,
                LocalDateTime.now().plusDays(1));

        assertThrows(elotech.taskmanager.exception.NotFoundException.class, () -> taskService.update(777L, request));
    }

    @Test
    void updateShouldThrowForbiddenWhenCurrentUserIsNotProjectMember() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);
        Task task = buildTask(90L, "Task", TaskStatusEnum.IN_PROGRESS, PriorityEnum.LOW, project, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(90L)).thenReturn(Optional.of(task));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(false);

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Titulo",
                "Descricao",
                10L,
                null,
                TaskStatusEnum.DONE,
                PriorityEnum.LOW,
                LocalDateTime.now().plusDays(1));

        assertThrows(ForbiddenException.class, () -> taskService.update(90L, request));
    }

    @Test
    void updateShouldThrowWhenClosingCriticalTaskWithoutProjectAdminRole() {
        setAuthenticatedUser("owner@email.com");
        User currentUser = buildUser(1L, "owner@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, currentUser);
        Task task = buildTask(91L, "Critica", TaskStatusEnum.IN_PROGRESS, PriorityEnum.CRITICAL, project, null);

        when(userRepository.findByEmail("owner@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(91L)).thenReturn(Optional.of(task));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Titulo",
                "Descricao",
                10L,
                null,
                TaskStatusEnum.DONE,
                PriorityEnum.CRITICAL,
                LocalDateTime.now().plusDays(1));

        assertThrows(BusinessRuleException.class, () -> taskService.update(91L, request));

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateShouldThrowWhenWipLimitIsExceeded() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        User assignee = buildUser(2L, "assignee@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);
        Task task = buildTask(92L, "Task", TaskStatusEnum.TODO, PriorityEnum.MEDIUM, project, assignee);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(92L)).thenReturn(Optional.of(task));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);
        when(userProjectRepository.existsByUser_IdAndProject_Id(2L, 10L)).thenReturn(true);
        when(taskRepository.countByProject_IdAndAssignee_IdAndStatusAndIdNot(10L, 2L, TaskStatusEnum.IN_PROGRESS, 92L))
                .thenReturn(5L);

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Titulo",
                "Descricao",
                10L,
                2L,
                TaskStatusEnum.IN_PROGRESS,
                PriorityEnum.HIGH,
                LocalDateTime.now().plusDays(1));

        assertThrows(BusinessRuleException.class, () -> taskService.update(92L, request));

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void updateShouldPersistWhenClosingCriticalTaskAsProjectAdmin() {
        setAuthenticatedUser("owner@email.com");
        User ownerAdmin = buildUser(1L, "owner@email.com", UserRoleEnum.ADMIN);
        User assignee = buildUser(2L, "assignee@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, ownerAdmin);
        Task task = buildTask(92L, "Task", TaskStatusEnum.IN_PROGRESS, PriorityEnum.CRITICAL, project, assignee);

        when(userRepository.findByEmail("owner@email.com")).thenReturn(Optional.of(ownerAdmin));
        when(taskRepository.findById(92L)).thenReturn(Optional.of(task));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);
        when(userProjectRepository.existsByUser_IdAndProject_Id(2L, 10L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Titulo atualizado",
                "Descricao",
                10L,
                2L,
                TaskStatusEnum.DONE,
                PriorityEnum.CRITICAL,
                LocalDateTime.now().plusDays(1));

        TaskResponse response = taskService.update(92L, request);

        assertEquals(TaskStatusEnum.DONE, response.getStatus());
        assertEquals(PriorityEnum.CRITICAL, response.getPriority());
    }

    @Test
    void updateShouldPersistWhenTaskIsNotCriticalEvenIfClosing() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);
        Task task = buildTask(93L, "Task", TaskStatusEnum.IN_PROGRESS, PriorityEnum.MEDIUM, project, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(93L)).thenReturn(Optional.of(task));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskUpdateRequest request = new TaskUpdateRequest(
                "Titulo atualizado",
                "Descricao",
                10L,
                null,
                TaskStatusEnum.DONE,
                PriorityEnum.HIGH,
                LocalDateTime.now().plusDays(1));

        TaskResponse response = taskService.update(93L, request);

        assertEquals(TaskStatusEnum.DONE, response.getStatus());
        assertEquals(PriorityEnum.HIGH, response.getPriority());
    }

    @Test
    void deleteShouldDeleteTaskWhenUserHasAccess() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);
        Task task = buildTask(93L, "Task", TaskStatusEnum.TODO, PriorityEnum.LOW, project, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(93L)).thenReturn(Optional.of(task));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(true);

        taskService.delete(93L);

        verify(taskRepository).delete(task);
    }

    @Test
    void deleteShouldThrowWhenTaskDoesNotExist() {
        when(taskRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(elotech.taskmanager.exception.NotFoundException.class, () -> taskService.delete(123L));
    }

    @Test
    void deleteShouldThrowForbiddenWhenUserHasNoAccessToProject() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        Project project = buildProject(10L, null);
        Task task = buildTask(93L, "Task", TaskStatusEnum.TODO, PriorityEnum.LOW, project, null);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.findById(93L)).thenReturn(Optional.of(task));
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 10L)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> taskService.delete(93L));
    }

    @Test
    void findAllShouldThrowBadRequestForInvalidSortBy() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));

        TaskListFiltersRequest filters = new TaskListFiltersRequest(
                null,
                null,
                null,
                null,
                null,
                "invalido",
                "asc",
                0,
                20);

        assertThrows(BadRequestException.class, () -> taskService.findAll(filters));
    }

    @Test
    void findAllShouldUseDeadlineAsDefaultSortAndNormalizePagination() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));

        Page<Task> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(taskRepository.findAllByFiltersForUser(anyLong(), any(), any(), any(), any(), any(),
                any(PageRequest.class)))
                .thenReturn(emptyPage);

        TaskListFiltersRequest filters = new TaskListFiltersRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                -1,
                0);

        taskService.findAll(filters);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(taskRepository).findAllByFiltersForUser(anyLong(), any(), any(), any(), any(), any(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void findAllShouldSupportPrioritySortDesc() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));

        Page<Task> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(taskRepository.findAllByFiltersForUser(anyLong(), any(), any(), any(), any(), any(),
                any(PageRequest.class)))
                .thenReturn(emptyPage);

        TaskListFiltersRequest filters = new TaskListFiltersRequest(
                null,
                null,
                null,
                null,
                null,
                "priority",
                "desc",
                0,
                20);

        taskService.findAll(filters);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(taskRepository).findAllByFiltersForUser(anyLong(), any(), any(), any(), any(), any(), captor.capture());
        assertEquals("DESC", captor.getValue().getSort().iterator().next().getDirection().name());
    }

    @Test
    void findAllShouldSupportCreatedAtSortAsc() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));

        Page<Task> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(taskRepository.findAllByFiltersForUser(anyLong(), any(), any(), any(), any(), any(),
                any(PageRequest.class)))
                .thenReturn(emptyPage);

        TaskListFiltersRequest filters = new TaskListFiltersRequest(
                null,
                null,
                null,
                null,
                null,
                "createdAt",
                "asc",
                0,
                20);

        taskService.findAll(filters);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(taskRepository).findAllByFiltersForUser(anyLong(), any(), any(), any(), any(), any(), captor.capture());
        assertEquals("createdAt: ASC", captor.getValue().getSort().toString());
    }

    @Test
    void searchByTextShouldReturnPagedResponse() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);

        Project project = buildProject(10L, null);
        Task task = buildTask(300L, "Refatorar", TaskStatusEnum.IN_PROGRESS, PriorityEnum.HIGH, project, null);
        Page<Task> page = new PageImpl<>(List.of(task), PageRequest.of(0, 20), 1);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.searchByTextForUser(1L, "ref", PageRequest.of(0, 20))).thenReturn(page);

        PagedResponse<TaskResponse> response = taskService.searchByText(" ref ", null, null);

        assertEquals(1, response.content().size());
        assertEquals("Refatorar", response.content().get(0).getTitle());
    }

    @Test
    void searchByTextShouldNormalizePageAndCapSize() {
        setAuthenticatedUser("user@email.com");
        User currentUser = buildUser(1L, "user@email.com", UserRoleEnum.MEMBER);

        Page<Task> page = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(currentUser));
        when(taskRepository.searchByTextForUser(anyLong(), any(), any(PageRequest.class))).thenReturn(page);

        taskService.searchByText("texto", -1, 999);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(taskRepository).searchByTextForUser(anyLong(), any(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    void searchByTextShouldThrowWhenNoAuthenticationInContext() {
        SecurityContextHolder.clearContext();

        assertThrows(BadRequestException.class, () -> taskService.searchByText("texto", 0, 10));
    }

    @Test
    void searchByTextShouldThrowWhenAuthenticationNameIsNull() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authentication.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(BadRequestException.class, () -> taskService.searchByText("texto", 0, 10));
    }

    @Test
    void searchByTextShouldThrowWhenAuthenticatedUserIsMissing() {
        setAuthenticatedUser("ghost@email.com");
        when(userRepository.findByEmail("ghost@email.com")).thenReturn(Optional.empty());

        assertThrows(elotech.taskmanager.exception.NotFoundException.class,
                () -> taskService.searchByText("texto", 0, 10));
    }

    private void setAuthenticatedUser(String email) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, "pass"));
    }

    private User buildUser(Long id, String email, UserRoleEnum role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Project buildProject(Long id, User owner) {
        Project project = new Project();
        project.setId(id);
        project.setOwner(owner);
        return project;
    }

    private Task buildTask(
            Long id,
            String title,
            TaskStatusEnum status,
            PriorityEnum priority,
            Project project,
            User assignee) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setStatus(status);
        task.setPriority(priority);
        task.setProject(project);
        task.setAssignee(assignee);
        return task;
    }
}
