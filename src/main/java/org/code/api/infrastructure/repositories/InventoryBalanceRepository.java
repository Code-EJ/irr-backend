package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.inventory.InventoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence queries for InventoryBalance; business callers must enforce explicit organization
 * scope.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {
  Optional<InventoryBalance> findByMaterialSubtypeId(UUID materialSubtypeId);

  boolean existsByMaterialSubtypeId(UUID materialSubtypeId);
}
