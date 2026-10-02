package org.code.api.controllers;

import jakarta.validation.Valid;
import org.code.api.domain.ports.AuthPort;
import org.code.api.dto.session.request.LoginRequestDTO;
import org.code.api.dto.session.response.LoginResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes authentication for active accounts through the supported versioned API.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestController
@RequestMapping("/api/v1/session")
public class SessionController {
  private final AuthPort auth;

  /**
   * @param auth existing-user authentication use case
   */
  public SessionController(AuthPort auth) {
    this.auth = auth;
  }

  /**
   * Authenticates an active account without exposing credential or account-existence details.
   *
   * @param data validated credentials
   * @return signed session token
   */
  @PostMapping("/authenticate")
  public ResponseEntity<LoginResponseDTO> authenticate(@Valid @RequestBody LoginRequestDTO data) {
    return ResponseEntity.ok(
        new LoginResponseDTO(auth.authenticate(data.email(), data.password())));
  }
}
