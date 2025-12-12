package org.code.api;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/*
  Mapeamento das java beans (dependencias) que não podem ser mapeadas automaticamente pelo
  spring mvc.
*/
@Configuration
public class IrrFactory {
  @Autowired
  private Environment environment;  

  /*
  Por padrão o spring security oferece algumas ferramentas já implementadas pra criptografia de senha.
  Por questão de compatibilidade entre diferentes versões do security, todas as alternativas implementam a 
  interface PasswordEncoder. No entanto, a mais utilizada é o BCryptPasswordEncoder, que utiliza o algoritmo bcrypt.
  */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /*
    Isso é provisório, por enquanto na fase inicial do projeto uma chave HMAC acredito que seja o suficiente.
    No entanto, no futuro seria recomendado ado utilizar chaves assimétricas (RSA ou EC) para assinar os tokens JWT.
  */
  @Bean
  public JwtEncoder jwtEncoder() {
    SecretKeySpec secretKey = new SecretKeySpec(
      environment.getProperty("irr.auth.secret").getBytes(),
      "HmacSha256"
    );

    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(secretKey).build())));
  }
}
