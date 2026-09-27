package org.code.api.domain.exception;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.code.api.domain.models.user.Session;

/**
 * Auth Error boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public class AuthError extends IrrApplicationException {

  public AuthError(String message, Throwable throwable) {
    super("Auth", message, throwable);
  }

  public AuthError(String message) {
    super("Auth", message);
  }

  public static class Unauthorized extends AuthError {
    public Unauthorized(String message) {
      super(message);
    }
  }

  public static class InternalServerError extends AuthError {
    public InternalServerError(String message) {
      super(message);
    }
  }

  /** Legacy provisioning error for an invalid creator identifier. */
  @Getter
  @Setter
  public static class CreatorUserInvalid extends AuthError {

    private String creatorUserId;
    private boolean invalidUUID;

    public CreatorUserInvalid(String creatorUserId, boolean invalidUUID, Throwable throwable) {
      super("Creator user is invalid", throwable);
      this.creatorUserId = creatorUserId;
      this.invalidUUID = invalidUUID;
    }

    public CreatorUserInvalid(String creatorUserId) {
      super("Creator user is invalid");
      this.creatorUserId = creatorUserId;
    }
  }

  /** Rejects credentials that exceed the password hashing adapter byte limit. */
  @Getter
  @Setter
  public static class PasswordTooLong extends AuthError {

    public final int passwordLength;

    public PasswordTooLong(int passwordLength, Throwable throwable) {
      super(
          String.format("Password's size is %s, which is more than 72 bytes", passwordLength),
          throwable);
      this.passwordLength = passwordLength;
    }

    public PasswordTooLong(int passwordLength) {
      super(String.format("Password's size is %s, which is more than 72 bytes", passwordLength));
      this.passwordLength = passwordLength;
    }
  }

  /** Indicates that the requested login address is already registered. */
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
     * Retains internal credential failure context; public responses must not reveal account
     * existence.
     */
    public WrongCredentials(String email, boolean isUserValid) {
      super("Wrong credentials");
      this.email = email;
      this.isUserValid = isUserValid;
    }
  }

  /**
   * Wraps invalid JWT structure, signature or verification failures; token content must not be
   * returned.
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

  /** Wraps token encoder failures behind the application token boundary. */
  public static class TokenCreationError extends AuthError {

    public TokenCreationError(String message, Throwable throwable) {
      super(message, throwable);
    }

    public TokenCreationError(Throwable throwable) {
      super(throwable.getMessage());
    }
  }

  /** Indicates that a verified session has expired; HTTP clients must authenticate again. */
  @Getter
  @Setter
  public static class ExpiredToken extends AuthError {

    private Session session;
    private Instant expiresAt;
    private Instant issuedAt;

    public ExpiredToken(Session session, Instant expiresAt, Instant issuedAt) {
      super("Expired token");
      this.session = session;
      this.expiresAt = expiresAt;
      this.issuedAt = issuedAt;
    }

    public ExpiredToken(Session session, long expiresAt, long issuedAt, Throwable throwable) {
      super("Expired token", throwable);
      this.session = session;
      this.expiresAt = Instant.ofEpochSecond(expiresAt);
      this.issuedAt = Instant.ofEpochSecond(issuedAt);
    }
  }
}
