package org.code.api.domain.exception;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.code.api.domain.models.user.Session;

public class AuthError extends IrrApplicationException {

    public AuthError(String message, Throwable throwable) {
        super("Auth", message, throwable);
    }

    public AuthError(String message) {
        super("Auth", message);
    }

    /**
     * Esse erro é lançado quando o usuário criador (createdBy) fornecido
     * para o cadastro de um novo usuário é inválido (não existe no banco de dados ou id é mal formatado).
     */
    @Getter
    @Setter
    public static class CreatorUserInvalid extends AuthError {

        private String creatorUserId;
        private boolean invalidUUID;

        public CreatorUserInvalid(
            String creatorUserId,
            boolean invalidUUID,
            Throwable throwable
        ) {
            super("Creator user is invalid", throwable);
            this.creatorUserId = creatorUserId;
            this.invalidUUID = invalidUUID;
        }

        public CreatorUserInvalid(String creatorUserId) {
            super("Creator user is invalid");
            this.creatorUserId = creatorUserId;
        }
    }

    /**
     * Este erro é lançado quando a senha fornecida para cadastro do usuário
     * excede o limite de 72 bytes imposto pela implementação do bcrypt do Spring SEcurity
     */
    @Getter
    @Setter
    public static class PasswordTooLong extends AuthError {

        public final int passwordLength;

        public PasswordTooLong(int passwordLength, Throwable throwable) {
            super(
                String.format(
                    "Password's size is %s, which is more than 72 bytes",
                    passwordLength
                ),
                throwable
            );
            this.passwordLength = passwordLength;
        }

        public PasswordTooLong(int passwordLength) {
            super(
                String.format(
                    "Password's size is %s, which is more than 72 bytes",
                    passwordLength
                )
            );
            this.passwordLength = passwordLength;
        }
    }

    /**
     * Esse error é lançado caso o email fornecido para cadastro do usuário já houver sido registrado.
     */
    @Getter
    @Setter
    public static class EmailOccupied extends AuthError {

        private String email;

        public EmailOccupied(String email, Throwable throwable) {
            super("Email occupied", throwable);
            this.email = email;
        }

        public EmailOccupied(String email) {
            super("Email occupied");
            this.email = email;
        }
    }

    @Getter
    @Setter
    public static class WrongCredentials extends AuthError {

        private String email;
        private boolean isUserValid;

        /**
         * Se isUserValid for true, o erro é devido à senha; se for false,
         * o usuário não está cadastrado no banco de dados.
         */
        public WrongCredentials(String email, boolean isUserValid) {
            super("Wrong credentials");
            this.email = email;
            this.isUserValid = isUserValid;
        }
    }

    /**
     * Este erro é lançado quando o token fornecido na requisição é inválido. Exemplos:
     *  - não possui a quantidade correta de segmentos (header, payload, signature);
     *  - algoritmo de assinatura incorreto;
     *  - secret (segredo) incorreto;
     *  - ou o token não é um JWT/hash válido.
     */
    @Getter
    @Setter
    public static class InvalidToken extends AuthError {

        private String token;

        public InvalidToken(String token, Throwable throwable) {
            super("Invalid token", throwable);
            this.token = token;
        }

        public InvalidToken(String token) {
            super("Invalid token");
            this.token = token;
        }
    }

    /**
     * Este erro é lançado quando há um problema durante a criação do token JWT.
     * No geral, esse erro é crítico e é lançado pelo Nimbus, eu só faço o encapsulamento dele
     * em uma exceção da aplicação, pra manter o padrão e o desacoplamento da lógica (a gente considera que esse erro faz parte
     * de um comportamento esperado do token port).
     */
    public static class TokenCreationError extends AuthError {

        public TokenCreationError(String message, Throwable throwable) {
            super(message, throwable);
        }

        public TokenCreationError(Throwable throwable) {
            super(throwable.getMessage());
        }
    }

    /**
     * Esta exceção é lançada quando expiresAt (timestamp de expiração do token)
     * é anterior ao tempo atual — ou seja, o token está expirado.
     *
     * TODO: implementar período de tolerância para renovação do token (renew grace period).
     */
    @Getter
    @Setter
    public static class ExpiredToken extends AuthError {

        private Session session;
        private Instant expiresAt;
        private Instant issuedAt;

        public ExpiredToken(
            Session session,
            Instant expiresAt,
            Instant issuedAt
        ) {
            super("Expired token");
            this.session = session;
            this.expiresAt = expiresAt;
            this.issuedAt = issuedAt;
        }

        public ExpiredToken(
            Session session,
            Instant expiresAt,
            Instant issuedAt,
            Throwable throwable
        ) {
            super("Expired token", throwable);
            this.session = session;
            this.expiresAt = expiresAt;
            this.issuedAt = issuedAt;
        }
    }
}
