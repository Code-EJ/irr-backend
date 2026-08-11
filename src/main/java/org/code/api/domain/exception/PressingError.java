package org.code.api.domain.exception;

import lombok.Getter;

import java.math.BigDecimal;
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

    /**
     * Lançada quando a prensagem não compacta o volume (finalVolumeM3 >= initialVolumeM3).
     * Mapeada para HTTP 400.
     */
    @Getter
    public static class InvalidCompaction extends PressingError {
        private final BigDecimal initialVolumeM3;
        private final BigDecimal finalVolumeM3;

        public InvalidCompaction(BigDecimal initialVolumeM3, BigDecimal finalVolumeM3) {
            super("Pressing must compact volume: finalVolumeM3 must be less than initialVolumeM3. " +
                  "Got initial=" + initialVolumeM3 + ", final=" + finalVolumeM3);
            this.initialVolumeM3 = initialVolumeM3;
            this.finalVolumeM3 = finalVolumeM3;
        }
    }
}
