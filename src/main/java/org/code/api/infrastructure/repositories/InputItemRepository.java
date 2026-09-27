package org.code.api.infrastructure.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.collection.InputItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence queries for InputItem; business callers must enforce explicit organization scope.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface InputItemRepository extends JpaRepository<InputItem, UUID> {
  Optional<InputItem> findByIdAndOrganizationId(UUID id, UUID organizationId);

  List<InputItem> findAllByCollectionId(UUID collectionId);

  List<InputItem> findAllByDonationId(UUID donationId);
}
