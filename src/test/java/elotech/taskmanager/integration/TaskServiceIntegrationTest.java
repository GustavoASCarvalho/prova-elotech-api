package elotech.taskmanager.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import elotech.taskmanager.dto.task.request.TaskCreateRequest;
import elotech.taskmanager.dto.task.response.TaskResponse;
import elotech.taskmanager.entity.Project;
import elotech.taskmanager.entity.Task;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.entity.UserProject;
import elotech.taskmanager.entity.UserProjectId;
import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import elotech.taskmanager.enums.UserRoleEnum;
import elotech.taskmanager.exception.ForbiddenException;
import elotech.taskmanager.repository.ProjectRepository;
import elotech.taskmanager.repository.TaskRepository;
import elotech.taskmanager.repository.UserProjectRepository;
import elotech.taskmanager.repository.UserRepository;
import elotech.taskmanager.service.TaskService;

@SpringBootTest
@Transactional
class TaskServiceIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserProjectRepository userProjectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @AfterEach
    void cleanUpSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createThenFindByIdShouldPersistTaskWhenUserIsProjectMember() {
        User owner = createUser("owner-it-", UserRoleEnum.ADMIN);
        Project project = createProject(owner, "Projeto Task IT");
        addMembership(owner, project);

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(owner.getEmail(), "pass"));

        TaskCreateRequest request = new TaskCreateRequest(
                "Task IT",
                "Descricao",
                project.getId(),
                null,
                TaskStatusEnum.TODO,
                PriorityEnum.HIGH,
                LocalDateTime.now().plusDays(3));

        TaskResponse created = taskService.create(request);
        TaskResponse fetched = taskService.findById(created.getId());

        assertEquals("Task IT", fetched.getTitle());
        assertEquals(project.getId(), fetched.getProjectId());
        assertTrue(taskRepository.findById(created.getId()).isPresent());
    }

    @Test
    void findByIdShouldThrowForbiddenWhenUserIsNotProjectMember() {
        User owner = createUser("owner-it-", UserRoleEnum.ADMIN);
        User outsider = createUser("outsider-it-", UserRoleEnum.MEMBER);

        Project project = createProject(owner, "Projeto Restrito IT");
        addMembership(owner, project);

        Task task = new Task();
        task.setTitle("Task Restrita");
        task.setDescription("Desc");
        task.setProject(project);
        task.setAssignee(null);
        task.setStatus(TaskStatusEnum.TODO);
        task.setPriority(PriorityEnum.LOW);
        task.setDeadline(LocalDateTime.now().plusDays(1));
        task = taskRepository.save(task);

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(outsider.getEmail(), "pass"));

        Long taskId = task.getId();
        assertThrows(ForbiddenException.class, () -> taskService.findById(taskId));
    }

    private User createUser(String prefix, UserRoleEnum role) {
        User user = new User();
        user.setName("User " + role.name());
        user.setEmail(prefix + UUID.randomUUID() + "@email.com");
        user.setPassword("encoded-pass");
        user.setRole(role);
        return userRepository.save(user);
    }

    private Project createProject(User owner, String name) {
        Project project = new Project();
        project.setName(name);
        project.setDescription("Descricao");
        project.setOwner(owner);
        return projectRepository.save(project);
    }

    private void addMembership(User user, Project project) {
        UserProject membership = new UserProject();
        membership.setId(new UserProjectId(user.getId(), project.getId()));
        membership.setUser(user);
        membership.setProject(project);
        userProjectRepository.save(membership);
    }
}
