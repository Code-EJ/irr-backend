package org.code.api.infrastructure;
import java.time.Duration;
import java.util.UUID;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.boot.actuate.data.redis.RedisHealthIndicator;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
/**
 * Verifies the configured Redis adapter uses authentication, TTLs and readiness connectivity.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class RedisInfrastructureIT extends PostgresIntegrationTest {
    @Autowired StringRedisTemplate redis;
    @Autowired RedisProperties properties;
    /** Verifies temporary state is isolated by key and expires at its configured TTL. */
    @Test void authenticatedConnectionStoresAndExpiresNamespacedKeys() {
        String key = "irr:test:" + UUID.randomUUID();
        try {
            redis.opsForValue().set(key, "fixture", Duration.ofSeconds(2));
            assertThat(redis.opsForValue().get(key)).isEqualTo("fixture");
            assertThat(redis.getExpire(key)).isBetween(0L,2L);
            await().atMost(Duration.ofSeconds(5)).until(() -> !Boolean.TRUE.equals(redis.hasKey(key)));
            assertThat(new RedisHealthIndicator(redis.getConnectionFactory()).health().getStatus().getCode()).isEqualTo("UP");
        } finally { redis.delete(key); }
    }
    /** Verifies a client without credentials cannot use the isolated Redis server. */
    @Test void unauthenticatedConnectionsAreRejected() {
        var config = new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
        var factory = new LettuceConnectionFactory(config);
        try {
            factory.afterPropertiesSet(); factory.start();
            assertThatThrownBy(() -> {
                try (var connection = factory.getConnection()) { connection.ping(); }
            }).isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasStackTraceContaining("NOAUTH");
        } finally { factory.destroy(); }
    }
}
