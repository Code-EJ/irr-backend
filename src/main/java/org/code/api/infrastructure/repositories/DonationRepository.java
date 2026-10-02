package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.donation.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for Donation.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {
  Optional<Donation> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Donation> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
