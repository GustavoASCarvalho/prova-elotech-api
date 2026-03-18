package elotech.taskmanager.dto.project.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @NotBlank(message = "name is required") @Size(max = 150, message = "name must have at most 150 characters") String name,

        @Size(max = 1000, message = "description must have at most 1000 characters") String description) {
}
