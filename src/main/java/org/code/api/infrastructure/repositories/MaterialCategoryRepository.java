package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.material.MaterialCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for MaterialCategory.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface MaterialCategoryRepository
    extends JpaRepository<MaterialCategory, UUID>, JpaSpecificationExecutor<MaterialCategory> {

  Optional<MaterialCategory> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<MaterialCategory> findAllByOrganizationId(UUID organizationId, Pageable pageable);

  boolean existsByNameAndOrganizationId(String name, UUID organizationId);
}
