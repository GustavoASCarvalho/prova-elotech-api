package elotech.taskmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import elotech.taskmanager.dto.auth.request.LoginRequest;
import elotech.taskmanager.dto.auth.request.RegisterRequest;
import elotech.taskmanager.dto.auth.response.AuthResponse;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.enums.UserRoleEnum;
import elotech.taskmanager.exception.BadRequestException;
import elotech.taskmanager.repository.UserRepository;
import elotech.taskmanager.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginShouldReturnAuthResponseWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@email.com", "senha");
        User user = buildUser(1L, "user@email.com", "hash", UserRoleEnum.MEMBER);

        when(userRepository.findByEmail("user@email.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(UserDetails.class), anyMap())).thenReturn("token-123");

        AuthResponse response = authService.login(request);

        assertEquals("token-123", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1L, response.getUserId());
        assertEquals("user@email.com", response.getEmail());
        assertEquals("MEMBER", response.getRole());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void loginShouldThrowBadRequestWhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("user@email.com", "senha-incorreta");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("invalid credentials"));

        assertThrows(BadRequestException.class, () -> authService.login(request));

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void loginShouldThrowBadRequestWhenAuthenticatedUserIsMissingInRepository() {
        LoginRequest request = new LoginRequest("ghost@email.com", "senha");
        when(userRepository.findByEmail("ghost@email.com")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    void registerShouldThrowBadRequestWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Nome", "user@email.com", "123456");
        when(userRepository.existsByEmail("user@email.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldCreateUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("Nome", "novo@email.com", "123456");

        when(userRepository.existsByEmail("novo@email.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("hash-123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(77L);
            return saved;
        });
        when(jwtService.generateToken(any(UserDetails.class), anyMap())).thenReturn("token-cadastro");

        AuthResponse response = authService.register(request);

        assertEquals("token-cadastro", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(77L, response.getUserId());
        assertEquals("novo@email.com", response.getEmail());
        assertEquals("MEMBER", response.getRole());
    }

    private User buildUser(Long id, String email, String password, UserRoleEnum role) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }
}
