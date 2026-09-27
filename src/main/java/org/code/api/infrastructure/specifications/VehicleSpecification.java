package org.code.api.infrastructure.specifications;

import org.code.api.domain.models.base.Vehicle;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;
import java.util.Locale;

/**
 * Reusable organization-owned vehicle query predicates.
  * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class VehicleSpecification {

    private VehicleSpecification() {
        // Utility class; no instances.
    }

    /**
 * Reusable organization-owned vehicle query predicates.
 */
    public static Specification<Vehicle> withOrganizationId(UUID organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    /**
 * Reusable organization-owned vehicle query predicates.
 */
    public static Specification<Vehicle> licensePlateContains(String licensePlate) {
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("licensePlate")), literalPattern(licensePlate), '\\');
    }

    /**
 * Reusable organization-owned vehicle query predicates.
 */
    public static Specification<Vehicle> modelContains(String model) {
        return (root, query, cb) ->
                cb.like(cb.upper(root.get("model")), literalPattern(model), '\\');
    }

    /**
 * Reusable organization-owned vehicle query predicates.
 */
    public static Specification<Vehicle> withIsActive(Boolean isActive) {
        return (root, query, cb) -> cb.equal(root.get("isActive"), isActive);
    }
    /** Builds a literal case-insensitive substring, escaping SQL LIKE metacharacters. */
    private static String literalPattern(String value) {
        return "%" + value.trim().toUpperCase(Locale.ROOT).replace("\\", "\\\\")
            .replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
