package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.sale.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for Sale.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {
  Optional<Sale> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Sale> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
