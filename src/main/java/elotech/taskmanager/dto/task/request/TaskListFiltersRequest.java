package elotech.taskmanager.dto.task.request;

import java.time.LocalDateTime;

import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;

public record TaskListFiltersRequest(
                TaskStatusEnum status,
                PriorityEnum priority,
                Long assigneeId,
                LocalDateTime deadlineFrom,
                LocalDateTime deadlineTo,
                String sortBy,
                String sortDir,
                Integer page,
                Integer size) {
}
