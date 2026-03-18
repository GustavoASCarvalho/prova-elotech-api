package elotech.taskmanager.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "name is required") @Size(max = 120, message = "name must have at most 120 characters") String name,

        @NotBlank(message = "email is required") @Email(message = "email must be valid") String email,

        @NotBlank(message = "password is required") @Size(min = 6, max = 255, message = "password must be between 6 and 255 characters") String password) {
}
