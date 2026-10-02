package org.code.api.infrastructure.repositories;

import java.util.List;
import java.util.UUID;
import org.code.api.domain.models.pressing.PressedBale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence queries for PressedBale; business callers must enforce explicit organization scope.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface PressedBaleRepository extends JpaRepository<PressedBale, UUID> {
  List<PressedBale> findAllByPressingId(UUID pressingId);
}
