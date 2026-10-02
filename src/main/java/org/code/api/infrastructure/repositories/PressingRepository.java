package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.pressing.Pressing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for Pressing.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface PressingRepository extends JpaRepository<Pressing, UUID> {
  Optional<Pressing> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Pressing> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
