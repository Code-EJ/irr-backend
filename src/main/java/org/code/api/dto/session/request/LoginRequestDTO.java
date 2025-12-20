package org.code.api.dto.session.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
    @NotBlank(message = "Email must not be blank") String email,
    @NotBlank(message = "Senha must not be blank") String senha
) {}
