package org.code.api.identity.recovery;

/**
 * Sends transactional text without coupling recovery policy to a vendor SDK.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public interface EmailSenderPort {
  /** Returns whether this environment explicitly enables transactional delivery. */
  boolean enabled();

  /** Sends a recovery link; implementations must never log the recipient or body. */
  void send(String recipient, String subject, String text);
}
