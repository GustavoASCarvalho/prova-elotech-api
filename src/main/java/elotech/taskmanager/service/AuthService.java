package elotech.taskmanager.service;

import elotech.taskmanager.dto.auth.request.LoginRequest;
import elotech.taskmanager.dto.auth.request.RegisterRequest;
import elotech.taskmanager.dto.auth.response.AuthResponse;

public interface AuthService {
        AuthResponse login(LoginRequest request);

        AuthResponse register(RegisterRequest request);
}
