package org.code.api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.code.api.filter.BearerFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Defines stateless bearer security, exact public routes and an explicit browser origin allowlist.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    /**
     * Installs the authentication filter and endpoint policy.
     * @param http security builder
     * @param bearer bearer identity filter
     * @param json error serializer
     * @param docsEnabled official documentation availability
     * @param origins comma-separated permitted browser origins
     * @return configured filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, BearerFilter bearer, ObjectMapper json,
        @Value("${springdoc.api-docs.enabled:true}") boolean docsEnabled,
        @Value("${irr.security.allowed-origins}") String origins) throws Exception {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(value -> !value.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Organization-Id", "Idempotency-Key"));
        cors.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        http.formLogin(form -> form.disable()).httpBasic(basic -> basic.disable())
            .csrf(csrf -> csrf.disable()).cors(config -> config.configurationSource(source))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.disable())
            .authorizeHttpRequests(access -> {
                access.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                access.requestMatchers(HttpMethod.POST, "/api/session/authenticate").permitAll();
                access.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll();
                if (docsEnabled) access.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**").permitAll();
                access.anyRequest().authenticated();
            })
            .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                response.setStatus(401); response.setContentType("application/json");
                json.writeValue(response.getOutputStream(), Map.of("error", "unauthorized", "message", "A valid bearer token is required"));
            }))
            .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Prevents Boot from registering the same security filter as a second servlet filter.
     * @param bearer security-chain filter
     * @return disabled standalone registration
     */
    @Bean
    public FilterRegistrationBean<BearerFilter> bearerRegistration(BearerFilter bearer) {
        var registration = new FilterRegistrationBean<>(bearer);
        registration.setEnabled(false);
        return registration;
    }
}
