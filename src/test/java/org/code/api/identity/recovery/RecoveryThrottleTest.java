package org.code.api.identity.recovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Checks account-independent abuse failure behavior without exposing raw email or address keys.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class RecoveryThrottleTest {
  /** Redis outages never become a permissive bypass for either public operation. */
  @Test
  void rejectsRedisOutages() {
    var redis = mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any(Object[].class)))
        .thenThrow(new DataAccessResourceFailureException("Synthetic outage"));
    var throttle = new RecoveryThrottle(redis);
    assertThatThrownBy(() -> throttle.request("fixture@example.test", "127.0.0.1"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("503");
    assertThatThrownBy(() -> throttle.confirm("127.0.0.1"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("503");
  }

  /** Exceeding the email limit produces neutral suppression, not an account-sensitive error. */
  @Test
  void suppressesLimitedRequestsAndRejectsConfirmationFloods() {
    var redis = mock(StringRedisTemplate.class);
    when(redis.execute(any(), anyList(), any(Object[].class))).thenReturn(21L);
    var throttle = new RecoveryThrottle(redis);
    assertThat(throttle.request("fixture@example.test", "127.0.0.1")).isFalse();
    assertThatThrownBy(() -> throttle.confirm("127.0.0.1"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("429");
  }
}
