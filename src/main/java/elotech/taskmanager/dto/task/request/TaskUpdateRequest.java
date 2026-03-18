package elotech.taskmanager.dto.task.request;

import java.time.LocalDateTime;

import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskUpdateRequest(
        @NotBlank(message = "title is required") @Size(max = 200, message = "title must have at most 200 characters") String title,

        @Size(max = 2000, message = "description must have at most 2000 characters") String description,

        @NotNull(message = "projectId is required") Long projectId,

        Long assigneeId,

        @NotNull(message = "status is required") TaskStatusEnum status,

        @NotNull(message = "priority is required") PriorityEnum priority,

        LocalDateTime deadline) {
}
