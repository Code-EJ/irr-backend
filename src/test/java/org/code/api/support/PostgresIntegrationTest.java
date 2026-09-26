package org.code.api.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.code.api.util.RSAKeysUtil;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Supplies a shared disposable PostgreSQL database and independent test credentials.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@SpringBootTest
public abstract class PostgresIntegrationTest {
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("irr_test").withUsername("irr_test").withPassword("isolated-test-only");
    static { DATABASE.start(); }

    /**
     * Binds this test context exclusively to disposable infrastructure.
     * @param registry Spring test property registry
     */
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
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
            Files.writeString(file, "-----BEGIN " + kind + " KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{10}).encodeToString(encoded)
                + "\n-----END " + kind + " KEY-----\n");
            return file.toUri().toString();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Cannot create isolated test key", exception);
        }
    }
}
