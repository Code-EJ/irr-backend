package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.sale.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório para {@link SaleItem}. Sem creator_id — itens são filhos de Sale.
 */
@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {
    List<SaleItem> findAllBySaleId(UUID saleId);
}
