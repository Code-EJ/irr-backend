package org.code.api;

import org.code.api.infrastructure.security.RSAConfigProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RSAConfigProps.class)
public class IrrApplication {
  public static void main(String[] args) {
    SpringApplication.run(IrrApplication.class, args);
  }
}
