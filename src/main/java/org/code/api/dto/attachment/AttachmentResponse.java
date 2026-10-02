package org.code.api.dto.attachment;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Public attachment metadata without persistence paths or user credentials.
 *
 * @param id attachment identity
 * @param fileName original display name
 * @param contentType server-detected media type
 * @param createdAt creation time
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record AttachmentResponse(
    UUID id, String fileName, String contentType, OffsetDateTime createdAt) {}
