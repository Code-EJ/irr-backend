package org.code.api.infrastructure.repositories;

import java.util.UUID;
import org.code.api.domain.enums.OperationType;
import org.code.api.domain.models.inventory.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence queries for InventoryLog; business callers must enforce explicit organization scope.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, UUID> {
  Page<InventoryLog> findAllByMaterialSubtypeId(UUID materialSubtypeId, Pageable pageable);

  Page<InventoryLog> findAllByOperationType(OperationType operationType, Pageable pageable);
}
