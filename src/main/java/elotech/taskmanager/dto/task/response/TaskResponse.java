package elotech.taskmanager.dto.task.response;

import java.time.LocalDateTime;

import elotech.taskmanager.enums.PriorityEnum;
import elotech.taskmanager.enums.TaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Long projectId;
    private Long assigneeId;
    private TaskStatusEnum status;
    private PriorityEnum priority;
    private LocalDateTime deadline;
}
