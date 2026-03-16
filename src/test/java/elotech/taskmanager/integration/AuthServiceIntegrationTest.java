package elotech.taskmanager.integration;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import elotech.taskmanager.dto.auth.request.LoginRequest;
import elotech.taskmanager.dto.auth.request.RegisterRequest;
import elotech.taskmanager.dto.auth.response.AuthResponse;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.repository.UserRepository;
import elotech.taskmanager.service.AuthService;

@SpringBootTest
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerThenLoginShouldPersistUserAndAuthenticate() {
        String email = "it-" + UUID.randomUUID() + "@email.com";
        RegisterRequest registerRequest = new RegisterRequest("Integration User", email, "123456");

        AuthResponse registerResponse = authService.register(registerRequest);

        assertNotNull(registerResponse.getToken());
        assertNotNull(registerResponse.getUserId());

        User persisted = userRepository.findByEmail(email).orElseThrow();
        assertNotEquals("123456", persisted.getPassword());
        assertTrue(passwordEncoder.matches("123456", persisted.getPassword()));

        AuthResponse loginResponse = authService.login(new LoginRequest(email, "123456"));

        assertNotNull(loginResponse.getToken());
        assertNotNull(loginResponse.getUserId());
    }
}
