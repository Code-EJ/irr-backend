package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.base.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonorRepository extends JpaRepository<Donor, UUID> {
    Optional<Donor> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<Donor> findAllByCreatorId(UUID creatorId, Pageable pageable);
    boolean existsByDocument(String document);
}
