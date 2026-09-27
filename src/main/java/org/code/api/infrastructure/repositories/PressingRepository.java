package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.pressing.Pressing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PressingRepository extends JpaRepository<Pressing, UUID> {
    Optional<Pressing> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Pressing> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
