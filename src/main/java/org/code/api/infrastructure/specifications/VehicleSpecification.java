package org.code.api.infrastructure.specifications;

import java.util.Locale;
import java.util.UUID;
import org.code.api.domain.models.base.Vehicle;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable organization-owned vehicle query predicates.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class VehicleSpecification {

  private VehicleSpecification() {
    // Utility class; no instances.
  }

  /** Restricts every result to the explicitly selected organization. */
  public static Specification<Vehicle> withOrganizationId(UUID organizationId) {
    return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
  }

  /** Matches a literal license-plate substring without interpreting SQL wildcards. */
  public static Specification<Vehicle> licensePlateContains(String licensePlate) {
    return (root, query, cb) ->
        cb.like(cb.upper(root.get("licensePlate")), literalPattern(licensePlate), '\\');
  }

  /** Matches a literal model substring using locale-independent case folding. */
  public static Specification<Vehicle> modelContains(String model) {
    return (root, query, cb) -> cb.like(cb.upper(root.get("model")), literalPattern(model), '\\');
  }

  /** Selects the requested active state while retaining the caller's organization predicate. */
  public static Specification<Vehicle> withIsActive(Boolean isActive) {
    return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
  }

  /** Builds a literal case-insensitive substring, escaping SQL LIKE metacharacters. */
  private static String literalPattern(String value) {
    return "%"
        + value
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        + "%";
  }
}
