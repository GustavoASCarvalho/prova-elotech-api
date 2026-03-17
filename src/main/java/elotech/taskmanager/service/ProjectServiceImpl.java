package elotech.taskmanager.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import elotech.taskmanager.dto.common.response.PagedResponse;
import elotech.taskmanager.dto.project.request.ProjectCreateRequest;
import elotech.taskmanager.dto.project.request.ProjectUpdateRequest;
import elotech.taskmanager.dto.project.response.ProjectResponse;
import elotech.taskmanager.entity.Project;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.entity.UserProject;
import elotech.taskmanager.entity.UserProjectId;
import elotech.taskmanager.exception.BadRequestException;
import elotech.taskmanager.exception.NotFoundException;
import elotech.taskmanager.repository.ProjectRepository;
import elotech.taskmanager.repository.UserProjectRepository;
import elotech.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private static final String PROJECT_NOT_FOUND = "Project not found";

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserProjectRepository userProjectRepository;

    public PagedResponse<ProjectResponse> findAll(Integer page, Integer size) {
        int currentPage = (page == null || page < 0) ? 0 : page;
        int pageSize = (size == null || size <= 0) ? 20 : Math.min(size, 200);

        Page<ProjectResponse> projectPage = projectRepository
                .findAll(PageRequest.of(currentPage, pageSize))
                .map(this::toDto);

        return PagedResponse.from(projectPage);
    }

    public ProjectResponse findById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(PROJECT_NOT_FOUND));
        return toDto(project);
    }

    public ProjectResponse create(ProjectCreateRequest dto) {
        User currentUser = getCurrentUser();
        Project project = new Project();
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setOwner(currentUser);
        Project savedProject = projectRepository.save(project);

        addMembershipIfMissing(currentUser, savedProject);

        return toDto(savedProject);
    }

    public ProjectResponse update(Long id, ProjectUpdateRequest dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(PROJECT_NOT_FOUND));
        applyFields(dto.getName(), dto.getDescription(), dto.getOwnerId(), project);
        return toDto(projectRepository.save(project));
    }

    public void delete(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new NotFoundException(PROJECT_NOT_FOUND);
        }
        projectRepository.deleteById(id);
    }

    public void addMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException(PROJECT_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (userProjectRepository.existsByUser_IdAndProject_Id(userId, projectId)) {
            throw new BadRequestException("User is already a member of this project");
        }

        UserProject membership = new UserProject();
        membership.setId(new UserProjectId(userId, projectId));
        membership.setUser(user);
        membership.setProject(project);
        userProjectRepository.save(membership);
    }

    public void removeMember(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException(PROJECT_NOT_FOUND));

        if (project.getOwner() != null && project.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Project owner cannot be removed from members");
        }

        long deleted = userProjectRepository.deleteByUser_IdAndProject_Id(userId, projectId);
        if (deleted == 0) {
            throw new NotFoundException("Membership not found for this user and project");
        }
    }

    private void applyFields(String name, String description, Long ownerId, Project project) {
        project.setName(name);
        project.setDescription(description);
        project.setOwner(resolveOwner(ownerId));
    }

    private User resolveOwner(Long ownerId) {
        if (ownerId == null) {
            return null;
        }
        return userRepository.findById(ownerId)
                .orElseThrow(() -> new BadRequestException("Owner not found"));
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

    private void addMembershipIfMissing(User user, Project project) {
        if (userProjectRepository.existsByUser_IdAndProject_Id(user.getId(), project.getId())) {
            return;
        }
        UserProject membership = new UserProject();
        membership.setId(new UserProjectId(user.getId(), project.getId()));
        membership.setUser(user);
        membership.setProject(project);
        userProjectRepository.save(membership);
    }

    private ProjectResponse toDto(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwner() != null ? project.getOwner().getId() : null)
                .build();
    }
}
