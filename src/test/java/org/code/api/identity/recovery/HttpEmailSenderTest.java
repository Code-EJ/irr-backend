package org.code.api.identity.recovery;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * Verifies the local HTTP adapter against a real socket stub without external email traffic.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
class HttpEmailSenderTest {
  /** Checks the selected provider's actual URI, authentication header and serialized wire body. */
  @Test
  void brevoProtocolUsesFixedHttpsEndpointAndDedicatedKey() throws Exception {
    var client = Mockito.mock(HttpClient.class);
    @SuppressWarnings("unchecked")
    HttpResponse<Void> result = Mockito.mock(HttpResponse.class);
    Mockito.when(result.statusCode()).thenReturn(201);
    Mockito.when(client.send(ArgumentMatchers.any(), ArgumentMatchers.<BodyHandler<Void>>any()))
        .thenReturn(result);
    var mail =
        new HttpEmailSender(
            new ObjectMapper(),
            "brevo",
            "synthetic-key",
            "sender@example.test",
            "IRR",
            "http://mailpit:8025",
            client);
    mail.send("recipient@example.test", "Recovery", "Synthetic link body");
    var captured = ArgumentCaptor.forClass(HttpRequest.class);
    Mockito.verify(client).send(captured.capture(), ArgumentMatchers.any());
    var request = captured.getValue();
    assertThat(request.uri().toString()).isEqualTo("https://api.brevo.com/v3/smtp/email");
    assertThat(request.headers().firstValue("api-key")).contains("synthetic-key");
    var subscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
    request
        .bodyPublisher()
        .orElseThrow()
        .subscribe(
            new Subscriber<ByteBuffer>() {
              @Override
              public void onSubscribe(Subscription subscription) {
                subscriber.onSubscribe(subscription);
              }

              @Override
              public void onNext(ByteBuffer buffer) {
                subscriber.onNext(List.of(buffer));
              }

              @Override
              public void onError(Throwable error) {
                subscriber.onError(error);
              }

              @Override
              public void onComplete() {
                subscriber.onComplete();
              }
            });
    var body =
        new ObjectMapper()
            .readTree(subscriber.getBody().toCompletableFuture().get(5, TimeUnit.SECONDS));
    assertThat(body.get("sender").get("email").asText()).isEqualTo("sender@example.test");
    assertThat(body.get("to").get(0).get("email").asText()).isEqualTo("recipient@example.test");
    assertThat(body.get("textContent").asText()).isEqualTo("Synthetic link body");
  }

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
