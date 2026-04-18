package org.code.api.domain.exception;

import org.code.api.domain.enums.UserType;
import lombok.Getter;

public class VehicleError extends RuntimeException {

    public VehicleError(String message) {
        super(message);
    }


    public static class InactiveVehicle extends VehicleError {
        public InactiveVehicle(Integer id) {
            super(String.format("O veículo de ID %d está inativo e não pode ser modificado.", id));
        }
    }

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
