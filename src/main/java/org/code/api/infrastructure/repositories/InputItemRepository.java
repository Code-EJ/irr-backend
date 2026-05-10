package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.collection.InputItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório para {@link InputItem}. Sem creator_id — itens são filhos de Collection/Donation.
 */
@Repository
public interface InputItemRepository extends JpaRepository<InputItem, UUID> {
    List<InputItem> findAllByCollectionId(UUID collectionId);
    List<InputItem> findAllByDonationId(UUID donationId);
}
