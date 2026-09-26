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
 * Exposes the current authentication and registration contract.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/session")
public class SessionController {

    private final AuthPort authPort;

    /**
     * Registers a representative using the current pre-production policy.
     * @param data validated registration fields
     * @return a created response containing the signed token
     */
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered")
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
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

    /**
     * Authenticates credentials and creates a signed session token.
     * @param data validated login credentials
     * @return a response containing the signed token
     */
    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponseDTO> authenticate(
        @Valid @RequestBody LoginRequestDTO data
    ) {
        String token = authPort.authenticate(data.email(), data.password());

        return ResponseEntity.status(HttpStatus.OK).body(
            new LoginResponseDTO(token)
        );
    }
}
