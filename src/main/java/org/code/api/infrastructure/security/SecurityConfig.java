package org.code.api.infrastructure.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.code.api.filter.BearerFilter;
import org.code.api.filter.LoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  @Autowired
  private BearerFilter bearerFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .formLogin(form -> form.disable())
      .httpBasic(basic -> basic.disable())

      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/session/**").permitAll()
        .anyRequest().authenticated()
      )

      .addFilterBefore(bearerFilter, UsernamePasswordAuthenticationFilter.class)
      .addFilterAfter(new LoggingFilter(), BearerFilter.class)

      .csrf(csrf -> csrf.disable())
      .cors(cors -> cors.disable());
    return http.build();
  }
}
