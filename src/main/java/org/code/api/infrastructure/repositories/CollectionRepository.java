package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.collection.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for Collection.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {
  Optional<Collection> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Collection> findAllByOrganizationId(UUID organizationId, Pageable pageable);

  boolean existsByVehicleId(UUID vehicleId);
}
