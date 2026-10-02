package org.code.api.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.code.api.util.RSAKeysUtil;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Supplies shared disposable PostgreSQL and Redis services and independent test credentials.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {
  private static final PostgreSQLContainer<?> DATABASE =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("irr_test")
          .withUsername("irr_test")
          .withPassword("isolated-test-only");
  private static final GenericContainer<?> REDIS =
      new GenericContainer<>(
              "redis:8.2-alpine@sha256:b51665e66f00759be7c3152ad5ac3c66fb2f619c13ef62dea7cc1f9914524635")
          .withExposedPorts(6379)
          .withCommand("redis-server", "--requirepass", "isolated-redis-only");

  static {
    DATABASE.start();
    REDIS.start();
  }

  /**
   * Binds this test context exclusively to disposable infrastructure.
   *
   * @param registry Spring test property registry
   */
  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add("spring.data.redis.password", () -> "isolated-redis-only");
    registry.add("irr.attachments.cleanup-enabled", () -> "false");
    registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
    registry.add("spring.datasource.username", DATABASE::getUsername);
    registry.add("spring.datasource.password", DATABASE::getPassword);
    registry.add("spring.flyway.baseline-on-migrate", () -> "false");
    registry.add("rsa.public-key", () -> key("PUBLIC", RSAKeysUtil.getPublicKey().getEncoded()));
    registry.add("rsa.private-key", () -> key("PRIVATE", RSAKeysUtil.getPrivateKey().getEncoded()));
  }

  private static String key(String kind, byte[] encoded) {
    try {
      Path file = Files.createTempFile("irr-test-", ".pem");
      file.toFile().deleteOnExit();
      Files.writeString(
          file,
          "-----BEGIN "
              + kind
              + " KEY-----\n"
              + Base64.getMimeEncoder(64, new byte[] {10}).encodeToString(encoded)
              + "\n-----END "
              + kind
              + " KEY-----\n");
      return file.toUri().toString();
    } catch (IOException exception) {
      throw new IllegalStateException("Cannot create isolated test key", exception);
    }
  }
}
