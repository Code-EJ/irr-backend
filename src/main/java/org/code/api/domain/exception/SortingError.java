package org.code.api.domain.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Exceções de negócio para operações de Triagem (Sorting).
 */
public class SortingError extends RuntimeException {

    public SortingError(String message) {
        super(message);
    }

    /**
     * Lançada quando nenhum registro de triagem é encontrado com o ID informado.
     * Mapeada para HTTP 404.
     */
    @Getter
    public static class NotFound extends SortingError {
        private final UUID sortingId;

        public NotFound(UUID sortingId) {
            super(String.format("Sorting record not found with ID: %s", sortingId));
            this.sortingId = sortingId;
        }
    }

    /**
     * Lançada quando um item de entrada referenciado não é encontrado.
     * Mapeada para HTTP 404.
     */
    @Getter
    public static class InputItemNotFound extends SortingError {
        private final UUID inputItemId;

        public InputItemNotFound(UUID inputItemId) {
            super(String.format("Input item not found with ID: %s", inputItemId));
            this.inputItemId = inputItemId;
        }
    }
}
