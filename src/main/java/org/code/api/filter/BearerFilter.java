package org.code.api.filter;

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

/**
 * Validates bearer sessions and populates the request security principal.
 * Exact status-only health routes bypass authentication for container probes.
 * The legacy session-prefix exemption is retained until the provisioning refactor.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@Component
public class BearerFilter implements Filter {

    @Autowired
    private AuthService authService;

    @org.springframework.beans.factory.annotation.Value("${springdoc.api-docs.enabled:true}")
    private boolean documentationEnabled;

    /** {@inheritDoc} */
    @Override
    public void doFilter(
        ServletRequest servletRequest,
        ServletResponse servletResponse,
        FilterChain filterChain
    ) throws IOException, ServletException {
        log.debug("Executing bearer authentication filter");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        if (path.startsWith("/api/session/") || path.equals("/actuator/health")
            || path.equals("/actuator/health/liveness") || path.equals("/actuator/health/readiness")
            || isDocumentationPath(path)) {
            filterChain.doFilter(request, response);
            return;
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

            String rolename = "ROLE_" + session.getUserRole().name();
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
     * Checks the exact documentation entrypoints and their static/configuration resources.
     * @param path request URI
     * @return whether enabled documentation may bypass business authentication
     */
    private boolean isDocumentationPath(String path) {
        return documentationEnabled && (path.equals("/swagger-ui.html")
            || path.startsWith("/swagger-ui/") || path.equals("/v3/api-docs")
            || path.equals("/v3/api-docs.yaml") || path.startsWith("/v3/api-docs/"));
    }

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
