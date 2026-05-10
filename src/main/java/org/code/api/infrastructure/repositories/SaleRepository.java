package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.sale.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {
    Optional<Sale> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<Sale> findAllByCreatorId(UUID creatorId, Pageable pageable);
}
