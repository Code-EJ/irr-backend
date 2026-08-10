package org.code.api.domain.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Exceções de negócio para operações de Prensagem (Pressing).
 */
public class PressingError extends RuntimeException {

    public PressingError(String message) {
        super(message);
    }

    /**
     * Lançada quando nenhum registro de prensagem é encontrado com o ID informado.
     * Mapeada para HTTP 404.
     */
    @Getter
    public static class NotFound extends PressingError {
        private final UUID pressingId;

        public NotFound(UUID pressingId) {
            super(String.format("Pressing record not found with ID: %s", pressingId));
            this.pressingId = pressingId;
        }
    }

    /**
     * Lançada quando um item triado referenciado não é encontrado.
     * Mapeada para HTTP 404.
     */
    @Getter
    public static class SortedItemNotFound extends PressingError {
        private final UUID sortedItemId;

        public SortedItemNotFound(UUID sortedItemId) {
            super(String.format("Sorted item not found with ID: %s", sortedItemId));
            this.sortedItemId = sortedItemId;
        }
    }
}
