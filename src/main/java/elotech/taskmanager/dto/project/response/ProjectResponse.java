package elotech.taskmanager.dto.project.response;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        Long ownerId) {
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Long getOwnerId() {
        return ownerId;
    }
}
