package org.code.api.infrastructure.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.material.MaterialSubtype;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Persists material subtype records and supports scoped database filtering.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface MaterialSubtypeRepository
    extends JpaRepository<MaterialSubtype, UUID>, JpaSpecificationExecutor<MaterialSubtype> {
  Optional<MaterialSubtype> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<MaterialSubtype> findAllByOrganizationId(UUID organizationId, Pageable pageable);

  Page<MaterialSubtype> findAllByTypeIdAndOrganizationId(
      UUID typeId, UUID organizationId, Pageable pageable);

  boolean existsByNameAndTypeIdAndOrganizationId(String name, UUID typeId, UUID organizationId);

  List<MaterialSubtype> findAllByTypeId(UUID typeId);

  List<MaterialSubtype> findAllByTypeIdIn(List<UUID> typeIds);
}
