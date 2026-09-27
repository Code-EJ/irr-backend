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
    Optional<Sorting> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Sorting> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    Page<Sorting> findAllByOrganizationIdAndSortingType(UUID organizationId, org.code.api.domain.enums.SortingType sortingType, Pageable pageable);
}

