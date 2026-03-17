package elotech.taskmanager.service;

import java.util.EnumMap;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import elotech.taskmanager.config.cache.CacheConfig;
import elotech.taskmanager.dto.task.response.TaskSummaryResponse;
import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import elotech.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskCacheServiceImpl implements TaskCacheService {

    private final TaskRepository taskRepository;

    @Cacheable(cacheNames = CacheConfig.PROJECT_SUMMARY_CACHE, key = "#projectId + ':' + #currentUserId")
    public TaskSummaryResponse getProjectSummaryCached(Long projectId, Long currentUserId) {
        EnumMap<TaskStatusEnum, Long> byStatus = new EnumMap<>(TaskStatusEnum.class);
        for (TaskStatusEnum status : TaskStatusEnum.values()) {
            byStatus.put(status, 0L);
        }

        EnumMap<PriorityEnum, Long> byPriority = new EnumMap<>(PriorityEnum.class);
        for (PriorityEnum priority : PriorityEnum.values()) {
            byPriority.put(priority, 0L);
        }

        taskRepository.countByStatusForProjectAndUser(projectId, currentUserId)
                .forEach(item -> byStatus.put(item.getStatus(), item.getTotal()));

        taskRepository.countByPriorityForProjectAndUser(projectId, currentUserId)
                .forEach(item -> byPriority.put(item.getPriority(), item.getTotal()));

        return TaskSummaryResponse.builder()
                .byStatus(byStatus)
                .byPriority(byPriority)
                .build();
    }
}
