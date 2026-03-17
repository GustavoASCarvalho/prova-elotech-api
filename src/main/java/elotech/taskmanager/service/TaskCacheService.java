package elotech.taskmanager.service;

import elotech.taskmanager.dto.task.response.TaskSummaryResponse;

public interface TaskCacheService {
    TaskSummaryResponse getProjectSummaryCached(Long projectId, Long currentUserId);
}

