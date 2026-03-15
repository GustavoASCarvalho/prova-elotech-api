package elotech.taskmanager.dto.task.request;

import java.time.LocalDateTime;

import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {

    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must have at most 200 characters")
    private String title;

    @Size(max = 2000, message = "description must have at most 2000 characters")
    private String description;

    @NotNull(message = "projectId is required")
    private Long projectId;

    private Long assigneeId;

    @NotNull(message = "status is required")
    private TaskStatusEnum status;

    @NotNull(message = "priority is required")
    private PriorityEnum priority;

    private LocalDateTime deadline;
}
