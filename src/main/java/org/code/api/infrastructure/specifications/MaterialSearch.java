package org.code.api.infrastructure.specifications;

import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds organization-scoped catalog searches before pagination and count queries. Name matching
 * uses database lowercase rules and treats wildcard characters literally.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class MaterialSearch {
  private MaterialSearch() {}

  /**
   * Combines the actor, optional parent and optional literal name fragment.
   *
   * @param <T> material entity type
   * @param organizationId authenticated actor identity
   * @param parentAttribute entity association name, or null at the category level
   * @param parentId optional parent identity
   * @param name optional case-insensitive substring, trimmed before matching
   * @return specification applied by the database before pagination
   */
  public static <T> Specification<T> matching(
      UUID organizationId, String parentAttribute, UUID parentId, String name) {
    return (root, query, builder) -> {
      var predicate = builder.equal(root.get("organizationId"), organizationId);
      if (parentId != null)
        predicate =
            builder.and(predicate, builder.equal(root.get(parentAttribute).get("id"), parentId));
      if (name != null && !name.isBlank()) {
        String literal = name.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        predicate =
            builder.and(
                predicate,
                builder.like(
                    builder.lower(root.get("name")),
                    builder.lower(builder.literal("%" + literal + "%")),
                    '!'));
      }
      return predicate;
    };
  }
}
