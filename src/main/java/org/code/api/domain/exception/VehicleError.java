package org.code.api.domain.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Namespace centralizador das exceções de negócio do contexto de Veículos.
 *
 * <p>Todas as classes internas devem ser interceptadas pelo {@code ErrorHandler}
 * para tradução em códigos HTTP adequados.</p>
 */
public class VehicleError extends RuntimeException {

    public VehicleError(String message) {
        super(message);
    }

    /**
     * Lançada quando o sistema detecta uma tentativa de modificação em um veículo inativo.
     * Mapeada para HTTP 422 (Unprocessable Entity).
     */
    public static class InactiveVehicle extends VehicleError {
        public InactiveVehicle(UUID id) {
            super(String.format("Vehicle with ID %s is inactive and cannot be modified.", id));
        }
    }

    /**
     * Lançada de forma proativa (fail-fast) ou reativa (DataIntegrityViolationException)
     * quando ocorre violação da unique constraint da coluna {@code license_plate}.
     * Mapeada para HTTP 409 (Conflict).
     */
    @Getter
    public static class PlateAlreadyExists extends VehicleError {

        private final String licensePlate;

        public PlateAlreadyExists(String licensePlate) {
            super("Vehicle plate already exists");
            this.licensePlate = licensePlate;
        }
    }

    /**
     * Lançada quando nenhum veículo é encontrado com o ID informado.
     * Mapeada para HTTP 404 (Not Found).
     */
    @Getter
    public static class NotFound extends VehicleError {

        private final UUID vehicleId;

        public NotFound(UUID vehicleId) {
            super("Vehicle not found");
            this.vehicleId = vehicleId;
        }
    }

    /**
     * Lançada quando um não-administrador tenta excluir um veículo que possui coletas vinculadas.
     * Mapeada para HTTP 409 (Conflict).
     */
    @Getter
    public static class HasCollectionBinding extends VehicleError {

        private final UUID vehicleId;

        public HasCollectionBinding(UUID vehicleId) {
            super("Permissão negada. Este veículo possui coletas vinculadas. Necessário requisitar ao instituto para prosseguir.");
            this.vehicleId = vehicleId;
        }
    }
}
