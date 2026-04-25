package org.code.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.ports.AuthPort;
import org.code.api.dto.session.login.request.LoginRequestDTO;
import org.code.api.dto.session.register.request.RegisterRequestDTO;
import org.code.api.dto.session.login.response.LoginResponseDTO;
import org.code.api.dto.session.register.response.RegisterResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST (Adaptador de Entrada) para autenticação e registro de usuários.
 *
 * <p>Expõe endpoints de login e registro, delegando toda a lógica de negócio e segurança
 * para a porta de domínio {@link org.code.api.domain.ports.AuthPort}. Atua apenas como
 * camada de transporte (HTTP), sem regras de negócio embutidas.
 *
 * @implNote O endpoint de login retorna HTTP 200 com o token JWT. O endpoint de registro
 * deve retornar HTTP 201 com o cabeçalho {@code Location} apontando para o recurso criado.
 * Autenticação, autorização (ex.: AOP/\{@code @PreAuthorize}) e tratamento global de exceções
 * são gerenciados pelo framework (ControllerAdvice).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Operações de login e gerenciamento de token")
public class AuthController {

    private final AuthPort authPort;

    @Operation(summary = "Autentica um usuário", description = "Gera um token JWT a partir de credenciais válidas.")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> authenticate(@Valid @RequestBody LoginRequestDTO data) {
        String token = authPort.authenticate(data.email(), data.senha());
        return ResponseEntity.ok(new LoginResponseDTO(token));
    }
}
