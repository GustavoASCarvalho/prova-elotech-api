package elotech.taskmanager.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import elotech.taskmanager.dto.auth.request.LoginRequest;
import elotech.taskmanager.dto.auth.request.RegisterRequest;
import elotech.taskmanager.dto.auth.response.AuthResponse;
import elotech.taskmanager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Operacoes de autenticacao e autorizacao")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Realizar login", description = "Autentica com email e senha e retorna JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Credenciais invalidas")
    })
    public AuthResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Registrar usuario", description = "Cria um usuario com role MEMBER e retorna JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos para registro")
    })
    public AuthResponse register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }
}

