package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.sorting.Sorting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SortingRepository extends JpaRepository<Sorting, UUID> {
    Optional<Sorting> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<Sorting> findAllByCreatorId(UUID creatorId, Pageable pageable);
}
