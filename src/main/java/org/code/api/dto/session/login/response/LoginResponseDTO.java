package org.code.api.dto.session.login.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padrão de autenticação contendo o token de acesso")
public record LoginResponseDTO(

        @Schema(description = "Token JWT criptografado para chamadas na API")
        String token,

        @Schema(description = "Tipo de autorização para o cabeçalho HTTP", example = "Bearer")
        String tipo
) {
    public LoginResponseDTO(String token) {
        this(token, "Bearer");
    }
}
