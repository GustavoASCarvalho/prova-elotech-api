package elotech.taskmanager.dto.task.response;

import java.util.Map;

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
public class TaskSummaryResponse {
    private Map<TaskStatusEnum, Long> byStatus;
    private Map<PriorityEnum, Long> byPriority;
}
