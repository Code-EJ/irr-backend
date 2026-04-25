package org.code.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.code.api.domain.ports.AuthPort;
import org.code.api.dto.session.register.request.RegisterRequestDTO;
import org.code.api.dto.session.register.response.RegisterResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Gestão de cadastro e perfis de usuários")
public class UserController {

    private final AuthPort authPort;

    @Operation(summary = "Registra um novo usuário", description = "Cria uma conta de acesso e retorna o token inicial.")
    @PostMapping
    public ResponseEntity<RegisterResponseDTO> register(@Valid @RequestBody RegisterRequestDTO data){
        String token = authPort
                .register(
                        data.nome(),
                        data.email(),
                        data.senha()
                );
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponseDTO(token));
    }

}
