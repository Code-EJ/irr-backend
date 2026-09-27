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
    Optional<Donor> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Donor> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    boolean existsByDocumentAndOrganizationId(String document, UUID organizationId);
}
