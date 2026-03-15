package elotech.taskmanager.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "seed")
public record SeedProperties(
        @NotBlank String defaultUserPassword) {
}
