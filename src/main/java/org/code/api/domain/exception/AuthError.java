package org.code.api.domain.exception;

import lombok.Getter;
import lombok.Setter;

public class AuthError extends IrrApplicationException {

  public AuthError(String message, Throwable throwable) {
    super("Auth", message, throwable);
  }

  public AuthError(String message) {
    super("Auth", message);
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
   * Esta exceção é lançada quando expiresAt (timestamp de expiração do token)
   * é anterior ao tempo atual — ou seja, o token está expirado.
   *
   * TODO: implementar período de tolerância para renovação do token (renew grace period).
   */
  @Getter
  @Setter
  public static class ExpiredToken extends AuthError {

    private String token;
    private long expiresAt;
    private long issuedAt;

    public ExpiredToken(String token, long expiresAt, long issuedAt) {
      super("Expired token");
      this.token = token;
      this.expiresAt = expiresAt;
      this.issuedAt = issuedAt;
    }

    public ExpiredToken(
      String token,
      long expiresAt,
      long issuedAt,
      Throwable throwable
    ) {
      super("Expired token", throwable);
      this.token = token;
      this.expiresAt = expiresAt;
      this.issuedAt = issuedAt;
    }
  }
}
