package elotech.taskmanager.dto.task.response;

import java.time.LocalDateTime;

import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Long projectId,
        Long assigneeId,
        TaskStatusEnum status,
        PriorityEnum priority,
        LocalDateTime deadline) {
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public TaskStatusEnum getStatus() {
        return status;
    }

    public PriorityEnum getPriority() {
        return priority;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }
}
