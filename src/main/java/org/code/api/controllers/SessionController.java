package org.code.api.controllers;

import jakarta.validation.Valid;
import java.util.Map;
import org.code.api.domain.ports.AuthPort;
import org.code.api.dto.session.request.LoginRequestDTO;
import org.code.api.dto.session.response.LoginResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Exposes existing-user authentication and an explicit retirement response for public signup.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping("/api/session")
public class SessionController {
    private final AuthPort auth;
    /** @param auth existing-user authentication use case */
    public SessionController(AuthPort auth) { this.auth = auth; }
    /**
     * Explains the pre-production contract replacement without creating an account.
     * @return the retired-endpoint response; unauthenticated callers receive 401 first
     */
    @io.swagger.v3.oas.annotations.Operation(deprecated = true, summary = "Retired self-registration")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Use administrator provisioning at POST /api/users")
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register() {
        return ResponseEntity.status(410).body(Map.of("error", "registration_retired", "message", "An administrator must create partners through POST /api/users"));
    }
    /**
     * Authenticates an active account without exposing credential or account-existence details.
     * @param data validated credentials
     * @return signed session token
     */
    @PostMapping("/authenticate")
    public ResponseEntity<LoginResponseDTO> authenticate(@Valid @RequestBody LoginRequestDTO data) {
        return ResponseEntity.ok(new LoginResponseDTO(auth.authenticate(data.email(), data.password())));
    }
}
