package org.code.api.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.ports.AuthPort;
import org.code.api.dto.session.request.LoginRequestDTO;
import org.code.api.dto.session.request.RegisterRequestDTO;
import org.code.api.dto.session.response.LoginResponseDTO;
import org.code.api.dto.session.response.RegisterResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para autenticação e registro de usuários.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/session")
public class SessionController {

    private final AuthPort authPort;

    @PostMapping("/register")
    public ResponseEntity<?> register(
        @Valid @RequestBody RegisterRequestDTO data
    ) {
        String token = authPort.register(
            data.fullName(),
            data.email(),
            data.password()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
            new RegisterResponseDTO(token)
        );
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(
        @Valid @RequestBody LoginRequestDTO data
    ) {
        String token = authPort.authenticate(data.email(), data.password());

        return ResponseEntity.status(HttpStatus.OK).body(
            new LoginResponseDTO(token)
        );
    }
}
