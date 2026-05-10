package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.donation.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonationRepository extends JpaRepository<Donation, UUID> {
    Optional<Donation> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<Donation> findAllByCreatorId(UUID creatorId, Pageable pageable);
}
