package org.code.api.dto.team.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TeamMemberResponseDTO(
    UUID id,
    String name,
    String role,
    Boolean isActive,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
