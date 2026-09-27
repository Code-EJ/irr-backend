package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.material.MaterialType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists material type records and supports scoped database filtering.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface MaterialTypeRepository extends JpaRepository<MaterialType, UUID>, JpaSpecificationExecutor<MaterialType> {
    Optional<MaterialType> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<MaterialType> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    Page<MaterialType> findAllByCategoryIdAndOrganizationId(UUID categoryId, UUID organizationId, Pageable pageable);
    boolean existsByNameAndCategoryIdAndOrganizationId(String name, UUID categoryId, UUID organizationId);
    List<MaterialType> findAllByCategoryId(UUID categoryId);
}
