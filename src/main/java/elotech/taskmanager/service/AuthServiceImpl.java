package elotech.taskmanager.service;

import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import elotech.taskmanager.dto.auth.request.LoginRequest;
import elotech.taskmanager.dto.auth.request.RegisterRequest;
import elotech.taskmanager.dto.auth.response.AuthResponse;
import elotech.taskmanager.entity.User;
import elotech.taskmanager.enums.UserRoleEnum;
import elotech.taskmanager.exception.BadRequestException;
import elotech.taskmanager.repository.UserRepository;
import elotech.taskmanager.security.JwtService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final AuthenticationManager authenticationManager;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;

        public AuthResponse login(LoginRequest request) {
                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(request.email(),
                                                        request.password()));
                } catch (AuthenticationException ex) {
                        throw new BadRequestException("Invalid email or password");
                }

                User user = userRepository.findByEmail(request.email())
                                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

                String token = jwtService.generateToken(
                                org.springframework.security.core.userdetails.User
                                                .withUsername(user.getEmail())
                                                .password(user.getPassword())
                                                .authorities("ROLE_" + user.getRole().name())
                                                .build(),
                                Map.of("role", user.getRole().name(), "userId", user.getId()));

                return new AuthResponse(
                                token,
                                "Bearer",
                                user.getId(),
                                user.getEmail(),
                                user.getRole().name());
        }

        public AuthResponse register(RegisterRequest request) {
                if (userRepository.existsByEmail(request.email())) {
                        throw new BadRequestException("Email is already in use");
                }

                User user = new User();
                user.setName(request.name());
                user.setEmail(request.email());
                user.setPassword(passwordEncoder.encode(request.password()));
                user.setRole(UserRoleEnum.MEMBER);
                User savedUser = userRepository.save(user);

                String token = jwtService.generateToken(
                                org.springframework.security.core.userdetails.User
                                                .withUsername(savedUser.getEmail())
                                                .password(savedUser.getPassword())
                                                .authorities("ROLE_" + savedUser.getRole().name())
                                                .build(),
                                Map.of("role", savedUser.getRole().name(), "userId", savedUser.getId()));

                return new AuthResponse(
                                token,
                                "Bearer",
                                savedUser.getId(),
                                savedUser.getEmail(),
                                savedUser.getRole().name());
        }
}
