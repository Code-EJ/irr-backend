package org.code.api.organizations.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Safe organization identity; classification never grants permissions.
 *
 * @param id organization identity
 * @param name display name
 * @param organizationType descriptive classification
 * @param createdAt recorded creation time
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record Organization(
    UUID id, String name, String organizationType, OffsetDateTime createdAt) {}
