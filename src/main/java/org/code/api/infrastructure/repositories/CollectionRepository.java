package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.collection.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, UUID> {
    Optional<Collection> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<Collection> findAllByCreatorId(UUID creatorId, Pageable pageable);
    boolean existsByVehicleId(UUID vehicleId);
}
