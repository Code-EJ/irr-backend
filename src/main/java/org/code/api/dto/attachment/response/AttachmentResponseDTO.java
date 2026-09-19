package org.code.api.dto.attachment.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.code.api.domain.models.base.Attachment;

public record AttachmentResponseDTO(
    UUID id,
    String fileName,
    String fileType,
    String storageUrl,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public AttachmentResponseDTO(Attachment attachment) {
        this(
            attachment.getId(),
            attachment.getFileName(),
            attachment.getFileType(),
            attachment.getStorageUrl(),
            attachment.getIsActive(),
            attachment.getCreatedAt(),
            attachment.getUpdatedAt()
        );
    }
}