package org.code.api;

import org.code.api.infrastructure.security.RSAConfigProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Irr Application boundary for the IRR application.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@SpringBootApplication
@EnableConfigurationProperties(RSAConfigProps.class)
public class IrrApplication {
  public static void main(String[] args) {
    SpringApplication.run(IrrApplication.class, args);
  }
}
