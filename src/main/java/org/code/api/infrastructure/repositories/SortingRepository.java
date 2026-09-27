package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.sorting.Sorting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for Sorting.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface SortingRepository extends JpaRepository<Sorting, UUID> {
  Optional<Sorting> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Sorting> findAllByOrganizationId(UUID organizationId, Pageable pageable);

  Page<Sorting> findAllByOrganizationIdAndSortingType(
      UUID organizationId, org.code.api.domain.enums.SortingType sortingType, Pageable pageable);
}
