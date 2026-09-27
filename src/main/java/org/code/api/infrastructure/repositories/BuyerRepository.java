package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.sale.Buyer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuyerRepository extends JpaRepository<Buyer, UUID> {
    Optional<Buyer> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Buyer> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
