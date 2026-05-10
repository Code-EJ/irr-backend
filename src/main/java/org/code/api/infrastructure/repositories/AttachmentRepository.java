package org.code.api.infrastructure.repositories;

import org.code.api.domain.models.base.Attachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    Optional<Attachment> findByIdAndCreatorId(UUID id, UUID creatorId);
    Page<Attachment> findAllByCreatorId(UUID creatorId, Pageable pageable);
}
