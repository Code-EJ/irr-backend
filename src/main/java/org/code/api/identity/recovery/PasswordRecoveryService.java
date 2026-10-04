package org.code.api.identity.recovery;

import java.net.URI;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.code.api.domain.ports.EncryptionPort;
import org.code.api.infrastructure.repositories.UserRepository;
import org.code.api.services.PasswordPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Issues one-use recovery authorization and changes credentials in one PostgreSQL transaction.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Service
public class PasswordRecoveryService {
  private final UserRepository users;
  private final EncryptionPort encryption;
  private final JdbcTemplate jdbc;
  private final EmailSenderPort mail;
  private final ThreadPoolTaskExecutor executor;
  private final TransactionTemplate transactions;
  private final RecoveryThrottle throttle;
  private final String baseUrl;
  private final int minutes;
  private final SecureRandom random = new SecureRandom();
  private static final Logger LOG = LoggerFactory.getLogger(PasswordRecoveryService.class);

  /** Binds existing identity adapters and validates operator-controlled recovery link settings. */
  public PasswordRecoveryService(
      UserRepository users,
      EncryptionPort encryption,
      JdbcTemplate jdbc,
      EmailSenderPort mail,
      ThreadPoolTaskExecutor recoveryExecutor,
      PlatformTransactionManager manager,
      RecoveryThrottle throttle,
      @Value("${irr.recovery.base-url}") String baseUrl,
      @Value("${irr.recovery.ttl-minutes}") int minutes) {
    this.users = users;
    this.encryption = encryption;
    this.jdbc = jdbc;
    this.mail = mail;
    this.executor = recoveryExecutor;
    this.transactions = new TransactionTemplate(manager);
    this.throttle = throttle;
    this.baseUrl = baseUrl;
    this.minutes = minutes;
    if (minutes < 5 || minutes > 60)
      throw new IllegalStateException("PASSWORD_RESET_TTL_MINUTES must be between 5 and 60");
    if (mail.enabled()) {
      var url = URI.create(baseUrl);
      boolean local = List.of("localhost", "127.0.0.1", "[::1]").contains(url.getHost());
      if (url.getHost() == null
          || url.getUserInfo() != null
          || url.getQuery() != null
          || url.getFragment() != null
          || !("https".equals(url.getScheme()) || ("http".equals(url.getScheme()) && local)))
        throw new IllegalStateException(
            "Configure a public HTTPS PASSWORD_RESET_BASE_URL or local loopback URL");
    }
  }

  /** Enqueues account lookup only after account-independent enabled and abuse checks. */
  public void request(String email, String address) {
    requireEnabled();
    if (!throttle.request(email, address)) return;
    try {
      executor.execute(() -> deliver(email.strip()));
    } catch (TaskRejectedException overflow) {
      LOG.warn("Recovery delivery queue is full");
    }
  }

  private void deliver(String email) {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    String digest = RecoveryThrottle.hash(token);
    try {
      String destination =
          transactions.execute(
              tx -> {
                var found =
                    jdbc.query(
                        "SELECT id FROM users WHERE email=? AND is_active",
                        (row, n) -> row.getObject(1, UUID.class),
                        email);
                if (found.isEmpty()) return null;
                var account = users.lockById(found.getFirst());
                if (account.isEmpty() || !account.get().getEmail().equals(email)) return null;
                var user = account.get();
                Instant expiry = Instant.now().plusSeconds(minutes * 60L);
                jdbc.update(
                    "INSERT INTO password_recovery_token(token_hash,user_id,issued_email,credential_version,expires_at) VALUES (?,?,?,?,?)",
                    digest,
                    user.getId(),
                    user.getEmail(),
                    user.getCredentialVersion(),
                    Timestamp.from(expiry));
                return user.getEmail();
              });
      if (destination != null)
        mail.send(
            destination,
            "IRR — Recuperação de senha",
            "Para redefinir sua senha, acesse:\n"
                + baseUrl
                + "#token="
                + token
                + "\nEste link expira em "
                + minutes
                + " minutos. Se não solicitou a alteração, ignore esta mensagem.");
    } catch (Exception failure) {
      // Never log exception causes: provider/database diagnostics may include sensitive inputs.
      LOG.warn("Recovery delivery failed");
      try {
        jdbc.update("DELETE FROM password_recovery_token WHERE token_hash=?", digest);
      } catch (Exception cleanupFailure) {
        LOG.warn("Recovery delivery cleanup failed");
      }
    }
  }

  /**
   * Consumes authorization atomically; failure never spends a valid link or changes credentials.
   */
  public void confirm(String token, String password, String address) {
    requireEnabled();
    throttle.confirm(address);
    PasswordPolicy.validate(password);
    String digest = RecoveryThrottle.hash(token);
    transactions.executeWithoutResult(
        tx -> {
          var ids =
              jdbc.query(
                  "SELECT user_id FROM password_recovery_token WHERE token_hash=?",
                  (row, n) -> row.getObject(1, UUID.class),
                  digest);
          if (ids.isEmpty()) throw invalid();
          var user = users.lockById(ids.getFirst()).orElseThrow(PasswordRecoveryService::invalid);
          var rows =
              jdbc.queryForList(
                  "SELECT issued_email,credential_version,expires_at,used_at FROM password_recovery_token WHERE token_hash=? FOR UPDATE",
                  digest);
          if (rows.isEmpty()) throw invalid();
          var row = rows.getFirst();
          if (row.get("used_at") != null
              || !((Timestamp) row.get("expires_at")).toInstant().isAfter(Instant.now())
              || ((Number) row.get("credential_version")).longValue() != user.getCredentialVersion()
              || !user.getEmail().equals(row.get("issued_email"))) throw invalid();
          if (encryption.compare(user.getPasswordHash(), password))
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Choose a different password");
          user.setPasswordHash(encryption.encrypt(password));
          user.setCredentialVersion(Math.incrementExact(user.getCredentialVersion()));
          user.setMustChangePassword(false);
          users.saveAndFlush(user);
          jdbc.update(
              "UPDATE password_recovery_token SET used_at=CURRENT_TIMESTAMP WHERE token_hash=?",
              digest);
        });
  }

  private void requireEnabled() {
    if (!mail.enabled())
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Recovery is temporarily unavailable");
  }

  private static ResponseStatusException invalid() {
    return new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Recovery link is invalid or expired; request another link");
  }

  /** Removes at most 1,000 expired records per interval, retaining no permanent token history. */
  @Scheduled(fixedDelayString = "PT1H")
  public void cleanup() {
    jdbc.update(
        "DELETE FROM password_recovery_token WHERE token_hash IN (SELECT token_hash FROM password_recovery_token WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '1 day' ORDER BY expires_at LIMIT 1000)");
  }
}
