package org.code.api.infrastructure.specifications;

import java.util.Locale;
import java.util.UUID;
import org.code.api.domain.models.material.MaterialCategory;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable category predicates; combine organization scope with any optional search condition.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class MaterialCategorySpecification {

  private MaterialCategorySpecification() {}

  public static Specification<MaterialCategory> withOrganizationId(UUID organizationId) {
    return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
  }

  public static Specification<MaterialCategory> nameContains(String name) {
    return (root, query, cb) -> cb.like(cb.upper(root.get("name")), literalPattern(name), '\\');
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
