package elotech.taskmanager.service;

import elotech.taskmanager.dto.common.response.PagedResponse;
import elotech.taskmanager.dto.task.request.TaskCreateRequest;
import elotech.taskmanager.dto.task.request.TaskListFiltersRequest;
import elotech.taskmanager.dto.task.request.TaskUpdateRequest;
import elotech.taskmanager.dto.task.response.TaskResponse;
import elotech.taskmanager.dto.task.response.TaskSummaryResponse;

public interface TaskService {
    TaskSummaryResponse getProjectSummary(Long projectId);

    PagedResponse<TaskResponse> searchByText(String text, Integer page, Integer size);

    PagedResponse<TaskResponse> findAll(TaskListFiltersRequest filters);

    TaskResponse findById(Long id);

    TaskResponse create(TaskCreateRequest dto);

    TaskResponse update(Long id, TaskUpdateRequest dto);

    void delete(Long id);
}

