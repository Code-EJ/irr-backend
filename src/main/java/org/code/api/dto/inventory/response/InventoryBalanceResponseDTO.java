package org.code.api.dto.inventory.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de resposta da Tabela de Leitura (saldo consolidado). Somente leitura.
 */
public record InventoryBalanceResponseDTO(
    UUID id,
    UUID materialSubtypeId,
    BigDecimal currentWeightKg,
    BigDecimal currentVolumeM3,
    OffsetDateTime lastUpdatedAt
) {}
