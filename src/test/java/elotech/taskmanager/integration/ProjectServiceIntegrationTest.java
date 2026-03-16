package elotech.taskmanager.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import elotech.taskmanager.dto.project.request.ProjectCreateRequest;
import elotech.taskmanager.dto.project.response.ProjectResponse;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.enums.UserRoleEnum;
import elotech.taskmanager.repository.UserProjectRepository;
import elotech.taskmanager.repository.UserRepository;
import elotech.taskmanager.service.ProjectService;

@SpringBootTest
@Transactional
class ProjectServiceIntegrationTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProjectRepository userProjectRepository;

    @AfterEach
    void cleanUpSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createShouldPersistProjectAndAddOwnerMembership() {
        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner-" + UUID.randomUUID() + "@email.com");
        owner.setPassword("encoded-pass");
        owner.setRole(UserRoleEnum.ADMIN);
        owner = userRepository.save(owner);

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(owner.getEmail(), "pass"));

        ProjectResponse response = projectService.create(new ProjectCreateRequest("Projeto IT", "Descricao IT"));

        assertNotNull(response.getId());
        assertEquals(owner.getId(), response.getOwnerId());

        boolean membershipExists = userProjectRepository.existsByUser_IdAndProject_Id(owner.getId(), response.getId());
        assertTrue(membershipExists);
    }

    @Test
    void addAndRemoveMemberShouldManageMembershipLifecycle() {
        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner2-" + UUID.randomUUID() + "@email.com");
        owner.setPassword("encoded-pass");
        owner.setRole(UserRoleEnum.ADMIN);
        owner = userRepository.save(owner);

        User member = new User();
        member.setName("Member");
        member.setEmail("member-" + UUID.randomUUID() + "@email.com");
        member.setPassword("encoded-pass");
        member.setRole(UserRoleEnum.MEMBER);
        member = userRepository.save(member);

        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken(owner.getEmail(), "pass"));

        ProjectResponse project = projectService.create(new ProjectCreateRequest("Projeto Membros", "Descricao"));

        projectService.addMember(project.getId(), member.getId());
        assertTrue(userProjectRepository.existsByUser_IdAndProject_Id(member.getId(), project.getId()));

        projectService.removeMember(project.getId(), member.getId());

        Long memberId = member.getId();
        Long projectId = project.getId();

        long remaining = userProjectRepository.findAll().stream()
                .filter(up -> up.getUser().getId().equals(memberId)
                        && up.getProject().getId().equals(projectId))
                .count();
        assertEquals(0L, remaining);
    }
}
