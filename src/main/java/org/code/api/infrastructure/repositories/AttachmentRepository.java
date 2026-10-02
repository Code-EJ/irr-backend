package org.code.api.infrastructure.repositories;

import java.util.Optional;
import java.util.UUID;
import org.code.api.domain.models.base.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence access for Attachment.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
  Optional<Attachment> findByIdAndOrganizationId(UUID id, UUID organizationId);

  Page<Attachment> findAllByOrganizationId(UUID organizationId, Pageable pageable);
}
