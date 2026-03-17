package elotech.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import elotech.taskmanager.dto.common.response.PagedResponse;
import elotech.taskmanager.dto.project.request.ProjectCreateRequest;
import elotech.taskmanager.dto.project.request.ProjectUpdateRequest;
import elotech.taskmanager.dto.project.response.ProjectResponse;
import elotech.taskmanager.entity.Project;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.entity.UserProject;
import elotech.taskmanager.enums.UserRoleEnum;
import elotech.taskmanager.exception.BadRequestException;
import elotech.taskmanager.exception.NotFoundException;
import elotech.taskmanager.repository.ProjectRepository;
import elotech.taskmanager.repository.UserProjectRepository;
import elotech.taskmanager.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProjectRepository userProjectRepository;

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectServiceImpl(projectRepository, userRepository, userProjectRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findAllShouldNormalizeDefaultPagination() {
        Project project = buildProject(10L, "Projeto A", "Desc", buildUser(1L, "owner@email.com", UserRoleEnum.ADMIN));
        Page<Project> page = new PageImpl<>(List.of(project), PageRequest.of(0, 20), 1);

        when(projectRepository.findAll(any(PageRequest.class))).thenReturn(page);

        PagedResponse<ProjectResponse> result = projectService.findAll(null, null);

        assertEquals(1, result.content().size());
        assertEquals("Projeto A", result.content().getFirst().getName());
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(projectRepository).findAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void findAllShouldNormalizeNegativePageAndCapMaximumSize() {
        Page<Project> page = new PageImpl<>(List.of(), PageRequest.of(0, 200), 0);
        when(projectRepository.findAll(any(PageRequest.class))).thenReturn(page);

        projectService.findAll(-1, 999);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(projectRepository).findAll(captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(200, captor.getValue().getPageSize());
    }

    @Test
    void findByIdShouldReturnProjectWhenExists() {
        User owner = buildUser(10L, "owner@email.com", UserRoleEnum.ADMIN);
        Project project = buildProject(30L, "Projeto", "Descricao", owner);
        when(projectRepository.findById(30L)).thenReturn(Optional.of(project));

        ProjectResponse response = projectService.findById(30L);

        assertEquals(30L, response.getId());
        assertEquals(10L, response.getOwnerId());
    }

    @Test
    void findByIdShouldThrowNotFoundWhenProjectDoesNotExist() {
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.findById(404L));
    }

    @Test
    void createShouldPersistProjectAndMembership() {
        setAuthenticatedUser("owner@email.com");
        User currentUser = buildUser(1L, "owner@email.com", UserRoleEnum.ADMIN);

        Project savedProject = buildProject(50L, "Novo", "Descricao", currentUser);

        when(userRepository.findByEmail("owner@email.com")).thenReturn(Optional.of(currentUser));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 50L)).thenReturn(false);

        ProjectResponse response = projectService.create(new ProjectCreateRequest("Novo", "Descricao"));

        assertEquals(50L, response.getId());
        assertEquals(1L, response.getOwnerId());
        verify(userProjectRepository).save(any(UserProject.class));
    }

    @Test
    void createShouldNotCreateMembershipWhenAlreadyExists() {
        setAuthenticatedUser("owner@email.com");
        User currentUser = buildUser(1L, "owner@email.com", UserRoleEnum.ADMIN);
        Project savedProject = buildProject(50L, "Novo", "Descricao", currentUser);

        when(userRepository.findByEmail("owner@email.com")).thenReturn(Optional.of(currentUser));
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(userProjectRepository.existsByUser_IdAndProject_Id(1L, 50L)).thenReturn(true);

        projectService.create(new ProjectCreateRequest("Novo", "Descricao"));

        verify(userProjectRepository, never()).save(any(UserProject.class));
    }

    @Test
    void createShouldThrowBadRequestWhenNoAuthenticatedUserInSecurityContext() {
        SecurityContextHolder.clearContext();

        assertThrows(BadRequestException.class, () -> projectService.create(new ProjectCreateRequest("Novo", "Desc")));
    }

    @Test
    void createShouldThrowNotFoundWhenAuthenticatedUserIsMissingInRepository() {
        setAuthenticatedUser("ghost@email.com");
        when(userRepository.findByEmail("ghost@email.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.create(new ProjectCreateRequest("Novo", "Desc")));
    }

    @Test
    void updateShouldThrowBadRequestWhenOwnerDoesNotExist() {
        Project existing = buildProject(20L, "Projeto", "Desc", null);
        when(projectRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ProjectUpdateRequest request = new ProjectUpdateRequest("Nome", "Nova desc", 999L);

        assertThrows(BadRequestException.class, () -> projectService.update(20L, request));

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    void updateShouldPersistWhenOwnerIdIsNull() {
        Project existing = buildProject(20L, "Projeto", "Desc", buildUser(1L, "owner@email.com", UserRoleEnum.ADMIN));
        when(projectRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUpdateRequest request = new ProjectUpdateRequest("Nome novo", "Nova desc", null);

        ProjectResponse response = projectService.update(20L, request);

        assertEquals("Nome novo", response.getName());
        assertEquals(null, response.getOwnerId());
    }

    @Test
    void updateShouldThrowNotFoundWhenProjectDoesNotExist() {
        when(projectRepository.findById(333L)).thenReturn(Optional.empty());

        ProjectUpdateRequest request = new ProjectUpdateRequest("Nome", "Desc", null);

        assertThrows(NotFoundException.class, () -> projectService.update(333L, request));
    }

    @Test
    void removeMemberShouldThrowWhenRemovingOwner() {
        User owner = buildUser(1L, "owner@email.com", UserRoleEnum.ADMIN);
        Project project = buildProject(200L, "Projeto", "Desc", owner);
        when(projectRepository.findById(200L)).thenReturn(Optional.of(project));

        assertThrows(BadRequestException.class, () -> projectService.removeMember(200L, 1L));

        verify(userProjectRepository, never()).deleteByUser_IdAndProject_Id(anyLong(), anyLong());
    }

    @Test
    void addMemberShouldThrowWhenUserAlreadyMember() {
        Project project = buildProject(11L, "Projeto", "Desc", null);
        User user = buildUser(9L, "membro@email.com", UserRoleEnum.MEMBER);

        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userProjectRepository.existsByUser_IdAndProject_Id(9L, 11L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> projectService.addMember(11L, 9L));

        verify(userProjectRepository, never()).save(any(UserProject.class));
    }

    @Test
    void addMemberShouldPersistMembershipWhenValid() {
        Project project = buildProject(11L, "Projeto", "Desc", null);
        User user = buildUser(9L, "membro@email.com", UserRoleEnum.MEMBER);

        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userProjectRepository.existsByUser_IdAndProject_Id(9L, 11L)).thenReturn(false);

        projectService.addMember(11L, 9L);

        verify(userProjectRepository).save(any(UserProject.class));
    }

    @Test
    void addMemberShouldThrowNotFoundWhenProjectDoesNotExist() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.addMember(999L, 1L));
    }

    @Test
    void addMemberShouldThrowNotFoundWhenUserDoesNotExist() {
        Project project = buildProject(11L, "Projeto", "Desc", null);
        when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
        when(userRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.addMember(11L, 123L));
    }

    @Test
    void deleteShouldThrowWhenProjectDoesNotExist() {
        when(projectRepository.existsById(999L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> projectService.delete(999L));

        verify(projectRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteShouldDeleteWhenProjectExists() {
        when(projectRepository.existsById(15L)).thenReturn(true);

        projectService.delete(15L);

        verify(projectRepository).deleteById(15L);
    }

    @Test
    void removeMemberShouldDeleteMembershipWhenNotOwnerAndMembershipExists() {
        User owner = buildUser(1L, "owner@email.com", UserRoleEnum.ADMIN);
        Project project = buildProject(200L, "Projeto", "Desc", owner);
        when(projectRepository.findById(200L)).thenReturn(Optional.of(project));
        when(userProjectRepository.deleteByUser_IdAndProject_Id(2L, 200L)).thenReturn(1L);

        projectService.removeMember(200L, 2L);

        verify(userProjectRepository).deleteByUser_IdAndProject_Id(2L, 200L);
    }

    @Test
    void removeMemberShouldThrowNotFoundWhenMembershipDoesNotExist() {
        User owner = buildUser(1L, "owner@email.com", UserRoleEnum.ADMIN);
        Project project = buildProject(200L, "Projeto", "Desc", owner);
        when(projectRepository.findById(200L)).thenReturn(Optional.of(project));
        when(userProjectRepository.deleteByUser_IdAndProject_Id(2L, 200L)).thenReturn(0L);

        assertThrows(NotFoundException.class, () -> projectService.removeMember(200L, 2L));
    }

    @Test
    void removeMemberShouldThrowNotFoundWhenProjectDoesNotExist() {
        when(projectRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> projectService.removeMember(123L, 1L));
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

    private Project buildProject(Long id, String name, String description, User owner) {
        Project project = new Project();
        project.setId(id);
        project.setName(name);
        project.setDescription(description);
        project.setOwner(owner);
        return project;
    }
}
