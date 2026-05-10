package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.sorting.SortedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório para {@link SortedItem}. Sem creator_id — itens são filhos de Sorting.
 */
@Repository
public interface SortedItemRepository extends JpaRepository<SortedItem, UUID> {
    List<SortedItem> findAllBySortingId(UUID sortingId);
}
