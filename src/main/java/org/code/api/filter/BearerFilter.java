package org.code.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.code.api.domain.models.user.Session;
import org.code.api.services.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves each bearer token to the active database identity and current role once per request.
 * Public access is decided by the security chain; no session-prefix exemption is granted.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Component
public class BearerFilter extends OncePerRequestFilter {
  private final AuthService auth;
  private final ObjectMapper json;
  private final boolean documentationEnabled;

  /**
   * Configures token resolution and response serialization.
   *
   * @param auth authentication use case
   * @param json JSON serializer
   * @param documentationEnabled whether official documentation is public
   */
  public BearerFilter(
      AuthService auth,
      ObjectMapper json,
      @Value("${springdoc.api-docs.enabled:true}") boolean documentationEnabled) {
    this.auth = auth;
    this.json = json;
    this.documentationEnabled = documentationEnabled;
  }

  /** {@inheritDoc} */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    if (path.isEmpty()) path = request.getRequestURI();
    return "OPTIONS".equals(request.getMethod())
        || ("POST".equals(request.getMethod()) && path.equals("/api/session/authenticate"))
        || ("GET".equals(request.getMethod())
            && (path.equals("/actuator/health")
                || path.equals("/actuator/health/liveness")
                || path.equals("/actuator/health/readiness")
                || (documentationEnabled
                    && (path.equals("/swagger-ui.html")
                        || path.startsWith("/swagger-ui/")
                        || path.equals("/v3/api-docs")
                        || path.equals("/v3/api-docs.yaml")
                        || path.startsWith("/v3/api-docs/")))));
  }

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null) {
      chain.doFilter(request, response);
      return;
    }
    if (!header.startsWith("Bearer ") || header.substring(7).isBlank()) {
      reject(response);
      return;
    }
    Session session;
    try {
      session = auth.getSessionDetails(header.substring(7).trim());
      if (session.isExpired()) {
        reject(response);
        return;
      }
    } catch (org.code.api.domain.exception.AuthError | IllegalArgumentException exception) {
      reject(response);
      return;
    }
    var identity =
        new UsernamePasswordAuthenticationToken(
            session.getId(),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + session.getUserRole().name())));
    identity.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(identity);
    request.setAttribute("session", session);
    try {
      chain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void reject(HttpServletResponse response) throws IOException {
    SecurityContextHolder.clearContext();
    response.setStatus(401);
    response.setContentType("application/json");
    json.writeValue(
        response.getOutputStream(),
        Map.of("error", "invalid_token", "message", "A valid bearer token is required"));
  }
}
