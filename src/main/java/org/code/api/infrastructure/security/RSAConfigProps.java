package org.code.api.infrastructure.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

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
