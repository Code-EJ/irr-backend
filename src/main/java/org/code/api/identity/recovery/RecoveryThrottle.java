package org.code.api.identity.recovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shares abuse limits across instances without placing recovery authorization in Redis.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
@lombok.RequiredArgsConstructor
public class RecoveryThrottle {
  private final StringRedisTemplate redis;
  private static final DefaultRedisScript<Long> LIMIT =
      new DefaultRedisScript<>(
          "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; return n",
          Long.class);

  /** Allows a bounded request independent of whether an account exists. */
  public boolean request(String email, String address) {
    return take("email:" + hash(email.strip().toLowerCase(Locale.ROOT)), 60, 1)
        && take("request-ip:" + hash(address), 60, 10)
        && take("delivery-budget", 86400, 300);
  }

  /** Rejects confirmation floods before hashing or account lookup. */
  public void confirm(String address) {
    if (!take("confirm-ip:" + hash(address), 60, 20))
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Try again later");
  }

  private boolean take(String key, int seconds, int limit) {
    try {
      Long count = redis.execute(LIMIT, List.of("irr:recovery:" + key), Integer.toString(seconds));
      if (count == null) throw new IllegalStateException("Unavailable rate limit");
      return count <= limit;
    } catch (DataAccessException | IllegalStateException failure) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Recovery is temporarily unavailable");
    }
  }

  /** Digests high entropy tokens and obscures email/address rate-limit keys. */
  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable");
    }
  }
}
