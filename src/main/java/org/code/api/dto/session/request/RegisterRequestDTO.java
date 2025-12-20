package org.code.api.dto.session.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequestDTO(
    @NotBlank(message = "Nome é um campo obrigatório") String nome,
    @NotBlank(message = "Email é um campo obrigatório") String email,
    @NotBlank(message = "Senha é um campo obrigatório") String senha
) {}
