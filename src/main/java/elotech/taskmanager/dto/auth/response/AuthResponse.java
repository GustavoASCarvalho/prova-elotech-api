package elotech.taskmanager.dto.auth.response;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String email,
        String role) {
    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
