package org.code.api.dto.team.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Typed Team Member Response API value.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record TeamMemberResponseDTO(
    UUID id,
    String name,
    String role,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
