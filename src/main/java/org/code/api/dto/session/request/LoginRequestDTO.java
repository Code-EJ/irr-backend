package org.code.api.dto.session.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must have at most 255 characters")
    String email,
    @NotBlank(message = "Senha must not be blank")
    @Size(max = 72, message = "Senha must have at most 72 characters")
    String senha
) {}
