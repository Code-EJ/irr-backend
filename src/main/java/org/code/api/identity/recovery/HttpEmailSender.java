package org.code.api.identity.recovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Brevo HTTPS delivery and an explicitly selected local Mailpit catcher; no automatic fallback.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
public class HttpEmailSender implements EmailSenderPort {
  private final String provider, key, from, name;
  private final URI endpoint;
  private final ObjectMapper json;
  private final HttpClient client;

  /** Validates selected delivery configuration at startup without printing secrets. */
  @org.springframework.beans.factory.annotation.Autowired
  public HttpEmailSender(
      ObjectMapper json,
      @Value("${irr.mail.provider}") String provider,
      @Value("${irr.mail.api-key}") String key,
      @Value("${irr.mail.from}") String from,
      @Value("${irr.mail.from-name}") String name,
      @Value("${irr.mail.mailpit-url}") String mailpit) {
    this(
        json,
        provider,
        key,
        from,
        name,
        mailpit,
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
  }

  /** Allows isolated transport verification without changing the production provider endpoint. */
  HttpEmailSender(
      ObjectMapper json,
      String provider,
      String key,
      String from,
      String name,
      String mailpit,
      HttpClient client) {
    this.client = client;
    this.json = json;
    this.provider = provider;
    this.key = key;
    this.from = from;
    this.name = name;
    if (!List.of("disabled", "brevo", "mailpit").contains(provider))
      throw new IllegalStateException("Select a supported MAIL_PROVIDER");
    if (!provider.equals("disabled") && !from.matches("[^\\s@]+@[^\\s@]+[.][^\\s@]+"))
      throw new IllegalStateException("Configure MAIL_FROM");
    if (provider.equals("brevo") && key.isBlank())
      throw new IllegalStateException("Configure MAIL_API_KEY");
    endpoint =
        provider.equals("brevo")
            ? URI.create("https://api.brevo.com/v3/smtp/email")
            : URI.create(mailpit + "/api/v1/send");
    if (provider.equals("mailpit")
        && (!List.of("http", "https").contains(endpoint.getScheme())
            || endpoint.getUserInfo() != null
            || endpoint.getQuery() != null
            || endpoint.getFragment() != null))
      throw new IllegalStateException("Configure a local MAILPIT_URL");
  }

  /** {@inheritDoc} */
  @Override
  public boolean enabled() {
    return !provider.equals("disabled");
  }

  /** {@inheritDoc} */
  @Override
  public void send(String recipient, String subject, String text) {
    if (!enabled()) throw new IllegalStateException("Email is disabled");
    try {
      Object body =
          provider.equals("brevo")
              ? Map.of(
                  "sender",
                  Map.of("email", from, "name", name),
                  "to",
                  List.of(Map.of("email", recipient)),
                  "subject",
                  subject,
                  "textContent",
                  text)
              : Map.of(
                  "From",
                  Map.of("Email", from, "Name", name),
                  "To",
                  List.of(Map.of("Email", recipient)),
                  "Subject",
                  subject,
                  "Text",
                  text);
      var request =
          HttpRequest.newBuilder(endpoint)
              .timeout(Duration.ofSeconds(10))
              .header("Content-Type", "application/json");
      if (provider.equals("brevo")) request.header("api-key", key);
      int status =
          client
              .send(
                  request
                      .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                      .build(),
                  HttpResponse.BodyHandlers.discarding())
              .statusCode();
      if (status < 200 || status >= 300) throw new IllegalStateException("Email delivery failed");
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Email delivery interrupted");
    } catch (Exception failure) {
      // Provider errors can contain recipients or credentials; never retain their diagnostic
      // payload.
      throw new IllegalStateException("Email delivery failed");
    }
  }
}
