package org.code.api.infrastructure.repositories;

import org.code.api.domain.enums.OperationType;
import org.code.api.domain.models.inventory.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositório para {@link InventoryLog} (Livro Razão — Append-Only).
 * Sem creator_id — logs são gerados automaticamente pelo sistema.
 */
@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, UUID> {
    Page<InventoryLog> findAllByMaterialSubtypeId(UUID materialSubtypeId, Pageable pageable);
    Page<InventoryLog> findAllByOperationType(OperationType operationType, Pageable pageable);
}
