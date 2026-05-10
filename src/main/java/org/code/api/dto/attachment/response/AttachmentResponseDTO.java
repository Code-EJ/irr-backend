package org.code.api.dto.attachment.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttachmentResponseDTO(
    UUID id,
    String fileName,
    String fileType,
    String storageUrl,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
