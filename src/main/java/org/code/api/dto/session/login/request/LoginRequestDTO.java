package org.code.api.dto.session.login.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

/**
 * Data Transfer Object (DTO) que encapsula o payload para a autenticação (login) de um usuário.
 *
 * <p>Implementado como um {@code record} para garantir imutabilidade absoluta dos dados de
 * entrada. Este objeto atua como o contrato da API com o mundo exterior (Front-end/Mobile),
 * garantindo que apenas propriedades estritamente necessárias para a autenticação cheguem à
 * camada de domínio.
 *
 * @param email E-mail corporativo ou pessoal usado como login.
 * @param senha Credencial de acesso do usuário.
 */
@Schema(description = "Payload de requisição para autenticação de usuário")
public record LoginRequestDTO(

        @Schema(
                description = "E-mail corporativo do usuário",
                example = "enzo.ribas@codejr.com"
        )
        @NotBlank(message = "{api.user.email.notblank}")
        @Email(message = "{api.user.email.invalid}")
        String email,

        @Schema(
                description = "Senha de acesso do usuário",
                example = "S3nh@F0rt3!" // Texto claro aqui
        )
        @NotBlank(message = "{api.user.senha.notblank}")
        String senha
) {
    @Override
    public String toString() {
        return "AuthLoginRequestDTO[email=" + email + ", senha=***MASKED***]";
    }
}
