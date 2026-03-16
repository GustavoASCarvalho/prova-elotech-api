package elotech.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import elotech.taskmanager.dto.task.response.TaskSummaryResponse;
import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import elotech.taskmanager.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskCacheServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskCacheService taskCacheService;

    @Test
    void getProjectSummaryCachedShouldReturnZeroForMissingBucketsAndApplyRepositoryCounts() {
        TaskRepository.StatusCountProjection statusInProgress = new TaskRepository.StatusCountProjection() {
            @Override
            public TaskStatusEnum getStatus() {
                return TaskStatusEnum.IN_PROGRESS;
            }

            @Override
            public Long getTotal() {
                return 3L;
            }
        };

        TaskRepository.PriorityCountProjection priorityHigh = new TaskRepository.PriorityCountProjection() {
            @Override
            public PriorityEnum getPriority() {
                return PriorityEnum.HIGH;
            }

            @Override
            public Long getTotal() {
                return 4L;
            }
        };

        when(taskRepository.countByStatusForProjectAndUser(10L, 1L)).thenReturn(List.of(statusInProgress));
        when(taskRepository.countByPriorityForProjectAndUser(10L, 1L)).thenReturn(List.of(priorityHigh));

        TaskSummaryResponse response = taskCacheService.getProjectSummaryCached(10L, 1L);

        assertEquals(0L, response.getByStatus().get(TaskStatusEnum.TODO));
        assertEquals(3L, response.getByStatus().get(TaskStatusEnum.IN_PROGRESS));
        assertEquals(0L, response.getByStatus().get(TaskStatusEnum.DONE));

        assertEquals(0L, response.getByPriority().get(PriorityEnum.LOW));
        assertEquals(4L, response.getByPriority().get(PriorityEnum.HIGH));
        assertEquals(0L, response.getByPriority().get(PriorityEnum.CRITICAL));

        verify(taskRepository).countByStatusForProjectAndUser(10L, 1L);
        verify(taskRepository).countByPriorityForProjectAndUser(10L, 1L);
    }
}
