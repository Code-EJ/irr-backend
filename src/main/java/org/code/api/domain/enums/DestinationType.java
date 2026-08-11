package org.code.api.domain.enums;

/**
 * Destino do material após triagem/prensagem.
 * STOCK  - permanece no estoque consolidado.
 * SALE   - vinculado a uma venda específica (destinationId = sale.id).
 * PRESSING - direcionado para prensagem (destinationId = pressing.id).
 */
public enum DestinationType {
    STOCK,
    SALE,
    PRESSING
}