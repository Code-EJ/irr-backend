package org.code.api.identity.recovery;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * Verifies the local HTTP adapter against a real socket stub without external email traffic.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class HttpEmailSenderTest {
  /** Mailpit receives a complete message; provider failure payloads never escape the adapter. */
  @Test
  void sendsTextAndRedactsFailure() throws Exception {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var captured = new AtomicReference<String>();
    server.createContext(
        "/api/v1/send",
        exchange -> {
          captured.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          exchange.sendResponseHeaders(captured.get().contains("Reject this") ? 500 : 200, -1);
          exchange.close();
        });
    server.start();
    try {
      var json = new ObjectMapper();
      var mail =
          new HttpEmailSender(
              json,
              "mailpit",
              "",
              "sender@example.test",
              "IRR",
              "http://127.0.0.1:" + server.getAddress().getPort());
      mail.send("recipient@example.test", "Recovery", "Synthetic text");
      var body = json.readTree(captured.get());
      assertThat(body.get("To").get(0).get("Email").asText()).isEqualTo("recipient@example.test");
      assertThat(body.get("Text").asText()).isEqualTo("Synthetic text");
      assertThatThrownBy(() -> mail.send("recipient@example.test", "Failure", "Reject this"))
          .hasMessage("Email delivery failed")
          .hasNoCause();
    } finally {
      server.stop(0);
    }
  }

  /** Disabled delivery has no default secret; Brevo requires explicit sender/key configuration. */
  @Test
  void requiresExplicitConfiguration() {
    assertThat(
            new HttpEmailSender(
                    new ObjectMapper(), "disabled", "", "", "IRR", "http://mailpit:8025")
                .enabled())
        .isFalse();
    assertThatThrownBy(
            () ->
                new HttpEmailSender(
                    new ObjectMapper(),
                    "brevo",
                    "",
                    "sender@example.test",
                    "IRR",
                    "http://mailpit:8025"))
        .hasMessage("Configure MAIL_API_KEY");
  }
}
