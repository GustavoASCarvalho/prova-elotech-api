package elotech.taskmanager.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import elotech.taskmanager.dto.common.response.PagedResponse;
import elotech.taskmanager.dto.project.request.ProjectCreateRequest;
import elotech.taskmanager.dto.project.request.ProjectUpdateRequest;
import elotech.taskmanager.dto.project.response.ProjectResponse;
import elotech.taskmanager.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Operacoes de gerenciamento de projetos")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @Operation(summary = "Listar projetos", description = "Retorna todos os projetos cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de projetos retornada com sucesso")
    })
    public PagedResponse<ProjectResponse> findAll(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        return projectService.findAll(page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar projeto por ID", description = "Retorna um projeto pelo identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projeto encontrado"),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado")
    })
    public ProjectResponse findById(@PathVariable Long id) {
        return projectService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar projeto", description = "Cria um novo projeto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Projeto criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos para criacao")
    })
    public ProjectResponse create(@RequestBody @Valid ProjectCreateRequest dto) {
        return projectService.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar projeto", description = "Atualiza os dados de um projeto existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Projeto atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos para atualizacao"),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado")
    })
    public ProjectResponse update(@PathVariable Long id, @RequestBody @Valid ProjectUpdateRequest dto) {
        return projectService.update(id, dto);
    }

    @PostMapping("/{projectId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Adicionar membro no projeto", description = "Adiciona um usuario como membro do projeto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Membro adicionado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Usuario ja e membro do projeto"),
            @ApiResponse(responseCode = "404", description = "Projeto ou usuario nao encontrado")
    })
    public void addMember(@PathVariable Long projectId, @PathVariable Long userId) {
        projectService.addMember(projectId, userId);
    }

    @DeleteMapping("/{projectId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover membro do projeto", description = "Remove um usuario da lista de membros do projeto")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Membro removido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Owner nao pode ser removido"),
            @ApiResponse(responseCode = "404", description = "Projeto, usuario ou membro nao encontrado")
    })
    public void removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        projectService.removeMember(projectId, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir projeto", description = "Remove um projeto pelo identificador")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Projeto excluido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado")
    })
    public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }
}

