package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.sale.Buyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for Buyer.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface BuyerRepository extends JpaRepository<Buyer, UUID> {
  Optional<Buyer> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Buyer> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
