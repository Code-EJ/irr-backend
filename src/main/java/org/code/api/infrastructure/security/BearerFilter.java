package org.code.api.infrastructure.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.exception.AuthError.ExpiredToken;
import org.code.api.domain.exception.AuthError.InvalidToken;
import org.code.api.domain.models.user.Session;
import org.code.api.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de segurança customizado responsável pela interceptação de requisições HTTP,
 * validação de tokens JWT (Bearer) e injeção do usuário no contexto de segurança global.
 *
 * <p>O fluxo de trabalho principal consiste em:
 * <ol>
 * <li>Ignorar rotas de bypass baseadas na whitelist do SecurityConfig.</li>
 * <li>Extrair e formatar o cabeçalho {@code Authorization}.</li>
 * <li>Validar a criptografia, expiração e integridade do token via {@link AuthService}.</li>
 * <li>Traduzir a {@code Session} decodificada para um {@link UsernamePasswordAuthenticationToken}
 * do Spring Security, populando as roles (autoridades) para viabilizar o uso do {@code @PreAuthorize}.</li>
 * </ol>
 *
 * @implNote Implementa uma regra de "Renewal Grace Period" que avisa o frontend
 * (via header {@code X-Token-Renewal}) sobre a necessidade de renovação do token.
 */
@Slf4j
@Component
public class BearerFilter extends OncePerRequestFilter {

    @Autowired
    private AuthService authService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * Método principal do filtro que intercepta requisições HTTP e aplica validações de segurança.
     *
     * @param request Objeto {@link HttpServletRequest} representando a requisição HTTP.
     * @param response Objeto {@link HttpServletResponse} representando a resposta HTTP.
     * @param filterChain Cadeia de filtros a ser executada.
     * @throws ServletException Caso ocorra um erro durante o processamento do filtro.
     * @throws IOException Caso ocorra um erro de I/O durante o processamento do filtro.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        log.debug("Bearer Filter executando");
        String path = request.getRequestURI();

        for (String publicRoute: SecurityConfig.PUBLIC_ROUTES) {
            if (pathMatcher.match(publicRoute, path)) {
                log.debug("Rota pública detectada, ignorando filtro Bearer: {}",
                        path);
                filterChain.doFilter(request, response);
                return;
            }
        }

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null) {
            sendRefusedResponse(
                    response,
                    "authorization_header_missing",
                    "Authorization header is missing",
                    HttpStatus.UNAUTHORIZED
            );
            return;
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            sendRefusedResponse(
                    response,
                    "invalid_authorization_header_format",
                    "Invalid authorization header format",
                    HttpStatus.UNAUTHORIZED
            );
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isBlank()) {
            sendRefusedResponse(
                    response,
                    "invalid_token",
                    "The provided token is invalid.",
                    HttpStatus.UNAUTHORIZED
            );
            return;
        }

        Session session;

        try {
            session = authService.getSessionDetails(token);
        } catch (ExpiredToken exception) {
            log.debug(
                    "Refused request {} due to expired token used, issued at: {}, expires at: {}",
                    request.getRemoteAddr(),
                    exception.getIssuedAt(),
                    exception.getExpiresAt()
            );
            sendExpiredTokenResponse(
                    response,
                    exception.getExpiresAt(),
                    exception.getIssuedAt()
            );
            return;
        } catch (InvalidToken invalidToken) {
            log.debug(
                    "Refused request from {} due to invalid token ",
                    request.getRemoteAddr(),
                    invalidToken
            );
            sendRefusedResponse(
                    response,
                    "invalid_token",
                    "The provided token is invalid.",
                    HttpStatus.UNAUTHORIZED
            );
            return;
        } catch (Exception exception) {
            sendRefusedResponse(
                    response,
                    "invalid_token",
                    "The provided token is invalid.",
                    HttpStatus.UNAUTHORIZED
            );
            return;
        }

        log.debug(
                "Session expiration status (Bearer Filter): {}",
                session.isExpired()
        );

        if (!session.isExpired()) {
            log.debug("Approved request from {}", session.getEmail());

            String rolename = "ROLE_" + session.getTipo().name();
            List<SimpleGrantedAuthority> authorityList = List.of(new SimpleGrantedAuthority(rolename));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    session.getId(),
                    null,
                    authorityList
            );

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            request.setAttribute("session", session);

            filterChain.doFilter(request, response);
            return;
        }

        if (session.isOnRenewalGrace()) {
            response.setHeader("X-Token-Renewal", "true");
            request.setAttribute("session", session);

            sendRefusedResponse(
                    response,
                    "token_on_renewal_grace",
                    "The provided token is on renewal grace period.",
                    HttpStatus.UNAUTHORIZED
            );
            return;
        }

        sendExpiredTokenResponse(
                response,
                session.getExpiresAt(),
                session.getIssuedAt()
        );
    }

    /**
     * Envia uma resposta HTTP indicando que o token expirou.
     *
     * @param response Objeto {@link HttpServletResponse} para enviar a resposta.
     * @param expiresAt Data e hora de expiração do token.
     * @param issuedAt Data e hora de emissão do token.
     * @throws IOException Caso ocorra um erro de I/O ao escrever a resposta.
     */
    private void sendExpiredTokenResponse(
            HttpServletResponse response,
            Instant expiresAt,
            Instant issuedAt
    ) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response
                .getWriter()
                .write(
                        String.format(
                                "{\"error\": \"expired_token\", \"message\": \"The provided token has expired.\", \"expires_at\": %d, \"issued_at\": %d}",
                                expiresAt.getEpochSecond(),
                                issuedAt.getEpochSecond()
                        )
                );
    }

    /**
     * Envia uma resposta HTTP indicando que a requisição foi recusada.
     *
     * @param response Objeto {@link HttpServletResponse} para enviar a resposta.
     * @param error_code Código de erro a ser retornado.
     * @param message Mensagem de erro a ser retornada.
     * @param httpStatusCode Código de status HTTP a ser retornado.
     * @throws IOException Caso ocorra um erro de I/O ao escrever a resposta.
     */
    private void sendRefusedResponse(
            HttpServletResponse response,
            String error_code,
            String message,
            HttpStatus httpStatusCode
    ) throws IOException {
        response.setStatus(httpStatusCode.value());
        response.setContentType("application/json");
        response
                .getWriter()
                .write(
                        String.format(
                                "{\"error\": \"%s\", \"message\": \"%s\"}",
                                error_code,
                                message
                        )
                );
    }
}