package org.code.api.infrastructure.specifications;

import org.code.api.domain.models.material.MaterialCategory;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Specifications reutilizáveis para filtragem dinâmica de {@link MaterialCategory}.
 */
public final class MaterialCategorySpecification {

    private MaterialCategorySpecification() {}

    public static Specification<MaterialCategory> withCreatorId(UUID creatorId) {
        return (root, query, cb) -> cb.equal(root.get("creator").get("id"), creatorId);
    }

    public static Specification<MaterialCategory> nameContains(String name) {
        return (root, query, cb) ->
            cb.like(cb.upper(root.get("name")), "%" + name.trim().toUpperCase() + "%");
    }
}
