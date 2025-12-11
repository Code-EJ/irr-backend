package org.code.api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/*
  Mapeamento das java beans (dependencias) que não podem ser mapeadas automaticamente pelo
  spring mvc.
*/
@Configuration
public class IrrFactory {
  /*
  Por padrão o spring security oferece algumas ferramentas já implementadas pra criptografia de senha.
  Por questão de compatibilidade entre diferentes versões do security, todas as alternativas implementam a 
  interface PasswordEncoder. No entanto, a mais utilizada é o BCryptPasswordEncoder, que utiliza o algoritmo bcrypt.
  */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
