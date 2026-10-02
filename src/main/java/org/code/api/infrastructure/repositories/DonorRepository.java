package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.base.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for Donor.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface DonorRepository extends JpaRepository<Donor, UUID> {
  Optional<Donor> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Donor> findAllByOrganizationId(UUID organizationId, Pageable pageable);

  boolean existsByDocumentAndOrganizationId(String document, UUID organizationId);
}
