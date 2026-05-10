package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.inventory.InventoryBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para {@link InventoryBalance} (Tabela de Leitura).
 * Sem creator_id, sem is_active — é um saldo consolidado por material.
 */
@Repository
public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, UUID> {
    Optional<InventoryBalance> findByMaterialSubtypeId(UUID materialSubtypeId);
    boolean existsByMaterialSubtypeId(UUID materialSubtypeId);
}
