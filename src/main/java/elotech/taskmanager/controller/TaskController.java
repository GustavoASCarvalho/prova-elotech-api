package elotech.taskmanager.controller;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
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
import elotech.taskmanager.dto.task.request.TaskCreateRequest;
import elotech.taskmanager.dto.task.request.TaskListFiltersRequest;
import elotech.taskmanager.dto.task.request.TaskUpdateRequest;
import elotech.taskmanager.dto.task.response.TaskResponse;
import elotech.taskmanager.dto.task.response.TaskSummaryResponse;
import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import elotech.taskmanager.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Operacoes de gerenciamento de tarefas")
public class TaskController {

        private final TaskService taskService;

        @GetMapping
        @Operation(summary = "Listar tarefas", description = "Retorna tarefas com filtros e ordenacao")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Lista de tarefas retornada com sucesso")
        })
        public PagedResponse<TaskResponse> findAll(
                        @RequestParam(required = false) TaskStatusEnum status,
                        @RequestParam(required = false) PriorityEnum priority,
                        @RequestParam(required = false) Long assigneeId,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineFrom,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadlineTo,
                        @RequestParam(required = false, defaultValue = "deadline") String sortBy,
                        @RequestParam(required = false, defaultValue = "asc") String sortDir,
                        @RequestParam(required = false, defaultValue = "0") Integer page,
                        @RequestParam(required = false, defaultValue = "50") Integer size) {
                TaskListFiltersRequest filters = new TaskListFiltersRequest(
                                status,
                                priority,
                                assigneeId,
                                deadlineFrom,
                                deadlineTo,
                                sortBy,
                                sortDir,
                                page,
                                size);
                return taskService.findAll(filters);
        }

        @GetMapping("/search")
        @Operation(summary = "Busca textual de tarefas", description = "Busca por texto no titulo ou descricao")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Texto de busca invalido")
        })
        public PagedResponse<TaskResponse> searchByText(
                        @RequestParam("text") String text,
                        @RequestParam(required = false, defaultValue = "0") Integer page,
                        @RequestParam(required = false, defaultValue = "20") Integer size) {
                return taskService.searchByText(text, page, size);
        }

        @GetMapping("/projects/{projectId}/summary")
        @Operation(summary = "Resumo de tarefas por projeto", description = "Retorna contadores por status e prioridade de um projeto")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso"),
                        @ApiResponse(responseCode = "403", description = "Sem acesso ao projeto")
        })
        public TaskSummaryResponse getProjectSummary(@PathVariable Long projectId) {
                return taskService.getProjectSummary(projectId);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Buscar tarefa por ID", description = "Retorna uma tarefa pelo identificador")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Tarefa encontrada"),
                        @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada")
        })
        public TaskResponse findById(@PathVariable Long id) {
                return taskService.findById(id);
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Criar tarefa", description = "Cria uma nova tarefa")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Tarefa criada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Dados invalidos para criacao")
        })
        public TaskResponse create(@RequestBody @Valid TaskCreateRequest dto) {
                return taskService.create(dto);
        }

        @PutMapping("/{id}")
        @Operation(summary = "Atualizar tarefa", description = "Atualiza os dados de uma tarefa existente")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Tarefa atualizada com sucesso"),
                        @ApiResponse(responseCode = "400", description = "Dados invalidos para atualizacao"),
                        @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada")
        })
        public TaskResponse update(@PathVariable Long id, @RequestBody @Valid TaskUpdateRequest dto) {
                return taskService.update(id, dto);
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        @Operation(summary = "Excluir tarefa", description = "Remove uma tarefa pelo identificador")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Tarefa excluida com sucesso"),
                        @ApiResponse(responseCode = "404", description = "Tarefa nao encontrada")
        })
        public void delete(@PathVariable Long id) {
                taskService.delete(id);
        }
}
