package org.code.api.infrastructure.repositories;

import java.util.List;
import java.util.UUID;
import org.code.api.domain.models.sorting.SortedItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence queries for SortedItem; business callers must enforce explicit organization scope.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface SortedItemRepository extends JpaRepository<SortedItem, UUID> {
  java.util.Optional<org.code.api.domain.models.sorting.SortedItem> findByIdAndOrganizationId(
      java.util.UUID id, java.util.UUID organizationId);

  List<SortedItem> findAllBySortingId(UUID sortingId);
}
