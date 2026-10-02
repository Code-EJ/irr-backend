package org.code.api.infrastructure.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RSAConfig Props boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@ConfigurationProperties(prefix = "rsa")
@Getter
@Setter
public class RSAConfigProps {
  private RSAPublicKey publicKey;
  private RSAPrivateKey privateKey;

  public RSAConfigProps(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
    this.publicKey = publicKey;
    this.privateKey = privateKey;
  }
}
