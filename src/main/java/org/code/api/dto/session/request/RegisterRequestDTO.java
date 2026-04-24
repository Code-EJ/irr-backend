package org.code.api.dto.session.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record RegisterRequestDTO(
    @NotBlank(message = "Nome é um campo obrigatório")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
    String nome,
    @NotBlank(message = "Email é um campo obrigatório")
    @Email(message = "Email deve ser válido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    String email,
    @NotBlank(message = "Senha é um campo obrigatório")
    @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
    String senha
) {}
