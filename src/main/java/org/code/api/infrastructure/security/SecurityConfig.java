package org.code.api.infrastructure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Classe de configuração de segurança para a aplicação.
 * Define as regras de segurança, incluindo autenticação, autorização e filtros personalizados.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private BearerFilter bearerFilter;

    /**
     * Rotas públicas que não requerem autenticação.
     */
    public static final String[] PUBLIC_ROUTES = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/v1/users",
            "/api/v1/auth/login",
    };

    /**
     * Configura a cadeia de filtros de segurança para a aplicação.
     *
     * @param http Objeto {@link HttpSecurity} usado para configurar as regras de segurança.
     * @return Uma instância configurada de {@link SecurityFilterChain}.
     * @throws Exception Caso ocorra algum erro durante a configuração.
     *
     * <p>As seguintes proteções são desativadas, pois não são aplicáveis a uma API REST:</p>
     * <ul>
     *   <li><b>CSRF:</b> Proteção contra requisições forjadas, desnecessária em APIs que usam JWT.</li>
     *   <li><b>Form Login:</b> Redirecionamento para formulário HTML, inadequado para APIs REST.</li>
     *   <li><b>HTTP Basic:</b> Envio de credenciais em Base64, substituído por autenticação JWT.</li>
     *   <li><b>CORS:</b> Gerenciamento de origens pelo Spring Security, configurado globalmente.</li>
     * </ul>
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))


                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(
                    bearerFilter,
                    UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        new LoggingFilter(),
                        BearerFilter.class
                );

        return http.build();
    }
}
