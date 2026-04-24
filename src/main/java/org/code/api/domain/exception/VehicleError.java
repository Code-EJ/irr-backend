package org.code.api.domain.exception;

import org.code.api.domain.enums.UserType;
import lombok.Getter;

/**
 * Namespace centralizador das regras de negócio violadas no contexto de Veículos.
 *
 * <p>O agrupamento de exceções estáticas dentro desta classe abstrata garante alta coesão e
 * facilita o rastreamento das falhas do domínio logístico. Todas as classes aqui devem ser
 * interceptadas pelo {@code GlobalExceptionHandler} para tradução em códigos HTTP adequados.
 *
 * @see org.code.api.domain.exception.IrrApplicationException
 */
public class VehicleError extends RuntimeException {

    public VehicleError(String message) {
        super(message);
    }


    /**
     * Lançada quando o sistema detecta uma tentativa de modificação em um veículo inativo.
     * Deve ser mapeada preferencialmente para HTTP 422 (Unprocessable Entity) ou 403 (Forbidden).
     */
    public static class InactiveVehicle extends VehicleError {
        public InactiveVehicle(Integer id) {
            super(String.format("O veículo de ID %d está inativo e não pode ser modificado.", id));
        }
    }

    /**
     * Lançada de forma proativa (Fail-fast) ou reativa (DataIntegrityViolationException) quando
     * ocorre violação da Unique Constraint da coluna "placa".
     * Mapeada para HTTP 409 (Conflict).
     */
    @Getter
    public static class PlateAlreadyExists extends VehicleError {

        private final String placa;

        public PlateAlreadyExists(String placa) {
            super("Vehicle plate already exists");
            this.placa = placa;
        }
    }

    @Getter
    public static class NotFound extends VehicleError {

        private final Integer vehicleId;

        public NotFound(Integer vehicleId) {
            super("Vehicle not found");
            this.vehicleId = vehicleId;
        }
    }

    @Getter
    public static class SessionUserNotFound extends VehicleError {

        private final String userId;

        public SessionUserNotFound(String userId) {
            super("Session user not found");
            this.userId = userId;
        }
    }

    @Getter
    public static class AccessDenied extends VehicleError {

        private final UserType userType;

        public AccessDenied(UserType userType) {
            super("Vehicle operation access denied");
            this.userType = userType;
        }
    }
}
