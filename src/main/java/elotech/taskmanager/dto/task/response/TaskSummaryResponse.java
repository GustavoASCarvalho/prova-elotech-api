package elotech.taskmanager.dto.task.response;

import java.util.Map;

import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;

public record TaskSummaryResponse(
        Map<TaskStatusEnum, Long> byStatus,
        Map<PriorityEnum, Long> byPriority) {
    public Map<TaskStatusEnum, Long> getByStatus() {
        return byStatus;
    }

    public Map<PriorityEnum, Long> getByPriority() {
        return byPriority;
    }
}
