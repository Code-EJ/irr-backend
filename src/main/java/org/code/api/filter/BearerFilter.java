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
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.exception.AuthError.ExpiredToken;
import org.code.api.domain.exception.AuthError.InvalidToken;
import org.code.api.domain.models.user.Session;
import org.code.api.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BearerFilter implements Filter {

    @Autowired
    private AuthService authService;

    @Override
    public void doFilter(
        ServletRequest servletRequest,
        ServletResponse servletResponse,
        FilterChain filterChain
    ) throws IOException, ServletException {
        log.debug("Bearer Filter executando");
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI();

        if (path.startsWith("/api/session/")) {
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

        if (!authorizationHeader.startsWith("Bearer")) {
            sendRefusedResponse(
                response,
                "invalid_authorization_header_format",
                "Invalid authorization header format",
                HttpStatus.UNAUTHORIZED
            );
            return;
        }

        try {
            String token = authorizationHeader.substring(7);
            Session session = authService.getSessionDetails(token);

            log.debug(
                "Session expiration status (Bearer Filter): {}",
                session.isExpired()
            );
            if (!session.isExpired()) {
                log.debug("Approved request from {}", session.getEmail());
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
            return;
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
        } catch (Exception exception) {
            sendRefusedResponse(
                response,
                "invalid_token",
                "The provided token is invalid",
                HttpStatus.UNAUTHORIZED
            );
            return;
        }
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
                    expiresAt,
                    issuedAt
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
