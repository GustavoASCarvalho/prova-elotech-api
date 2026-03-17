package elotech.taskmanager.service;

import elotech.taskmanager.dto.common.response.PagedResponse;
import elotech.taskmanager.dto.project.request.ProjectCreateRequest;
import elotech.taskmanager.dto.project.request.ProjectUpdateRequest;
import elotech.taskmanager.dto.project.response.ProjectResponse;

public interface ProjectService {
    PagedResponse<ProjectResponse> findAll(Integer page, Integer size);

    ProjectResponse findById(Long id);

    ProjectResponse create(ProjectCreateRequest dto);

    ProjectResponse update(Long id, ProjectUpdateRequest dto);

    void delete(Long id);

    void addMember(Long projectId, Long userId);

    void removeMember(Long projectId, Long userId);
}

