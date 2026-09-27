package org.code.api.organizations.api;
import java.util.Map;
import org.code.api.organizations.domain.OrganizationAccessDenied;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/**
 * Maps organization scope failures without disclosing foreign account or organization state.
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@RestControllerAdvice(assignableTypes=OrganizationController.class)
public class OrganizationErrorHandler {
    /** @return the same response for missing and inaccessible scope */
    @ExceptionHandler(OrganizationAccessDenied.class)
    public ResponseEntity<?> notFound(OrganizationAccessDenied error) { return ResponseEntity.status(404).body(Map.of("error","organization_not_found","message",error.getMessage())); }
    /** @return an English bounded-pagination validation response */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException error) { return ResponseEntity.badRequest().body(Map.of("error","invalid_page","message",error.getMessage())); }
}
