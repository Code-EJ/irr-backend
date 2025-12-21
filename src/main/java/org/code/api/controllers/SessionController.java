package org.code.api.controllers;

import org.code.api.dto.session.request.RegisterRequestDTO;
import org.code.api.dto.session.response.RegisterResponseDTO;
import org.code.api.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/api/session")
public class SessionController {
  @Autowired
  private AuthService authService;

  @PostMapping("/authenticate")
  public ResponseEntity<Object> authenticate() {
    return ResponseEntity.status(HttpStatus.OK).body("hello world dude");
  }

  @PostMapping("/register")
  public ResponseEntity<Object> register(
    @Valid
    @RequestBody(required = true)
    RegisterRequestDTO data
  ) {
    String token = authService.register(data.nome(), data.email(), data.senha());

    return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponseDTO(token));
  }
}
