package org.code.api.infrastructure.repositories;

import java.util.List;
import java.util.UUID;
import org.code.api.domain.models.sale.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence queries for SaleItem; business callers must enforce explicit organization scope.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {
  List<SaleItem> findAllBySaleId(UUID saleId);
}
