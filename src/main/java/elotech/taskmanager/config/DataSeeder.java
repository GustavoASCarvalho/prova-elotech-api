package elotech.taskmanager.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import elotech.taskmanager.config.properties.SeedProperties;
import elotech.taskmanager.entity.Project;
import elotech.taskmanager.entity.Task;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.entity.UserProject;
import elotech.taskmanager.entity.UserProjectId;
import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import elotech.taskmanager.enums.UserRoleEnum;
import elotech.taskmanager.repository.ProjectRepository;
import elotech.taskmanager.repository.TaskRepository;
import elotech.taskmanager.repository.UserProjectRepository;
import elotech.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserProjectRepository userProjectRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;

    @Override
    public void run(String... args) {
        User admin = findOrCreateUser("Admin", "admin@taskmanager.com", UserRoleEnum.ADMIN);
        User memberAna = findOrCreateUser("Ana", "ana@taskmanager.com", UserRoleEnum.MEMBER);
        User memberBruno = findOrCreateUser("Bruno", "bruno@taskmanager.com", UserRoleEnum.MEMBER);

        Project projectAlpha = findOrCreateProject("Projeto Alpha", "Projeto inicial para organizacao do backlog",
                admin);
        Project projectBeta = findOrCreateProject("Projeto Beta", "Projeto de melhorias internas", admin);

        addMembershipIfMissing(admin, projectAlpha);
        addMembershipIfMissing(memberAna, projectAlpha);
        addMembershipIfMissing(admin, projectBeta);
        addMembershipIfMissing(memberBruno, projectBeta);

        if (taskRepository.count() == 0) {
            createTask("Configurar ambiente", "Ajustar setup do projeto", projectAlpha,
                    TaskStatusEnum.IN_PROGRESS, PriorityEnum.HIGH, LocalDateTime.now().plusDays(2));
            createTask("Refatorar DTOs", "Separar request e response", projectAlpha,
                    TaskStatusEnum.TODO, PriorityEnum.MEDIUM, LocalDateTime.now().plusDays(5));
            createTask("Criar dashboard", "Implementar visao geral de tarefas", projectBeta,
                    TaskStatusEnum.TODO, PriorityEnum.LOW, LocalDateTime.now().plusDays(10));
        }
    }

    private User findOrCreateUser(String name, String email, UserRoleEnum role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(seedProperties.defaultUserPassword()));
            user.setRole(role);
            return userRepository.save(user);
        });
    }

    private Project findOrCreateProject(String name, String description, User owner) {
        return projectRepository.findByName(name)
                .orElseGet(() -> {
                    Project project = new Project();
                    project.setName(name);
                    project.setDescription(description);
                    project.setOwner(owner);
                    return projectRepository.save(project);
                });
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

    private void createTask(String title, String description, Project project, TaskStatusEnum status,
            PriorityEnum priority, LocalDateTime deadline) {
        Task task = new Task();
        task.setTitle(title);
        task.setDescription(description);
        task.setProject(project);
        task.setAssignee(null);
        task.setStatus(status);
        task.setPriority(priority);
        task.setDeadline(deadline);
        taskRepository.save(task);
    }
}
