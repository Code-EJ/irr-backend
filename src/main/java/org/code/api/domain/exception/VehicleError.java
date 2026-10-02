package org.code.api.domain.exception;

import java.util.UUID;
import lombok.Getter;

/**
 * Groups domain failures for vehicle operations and HTTP error translation.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public class VehicleError extends RuntimeException {

  public VehicleError(String message) {
    super(message);
  }

  /** Rejects mutation of an inactive vehicle. */
  public static class InactiveVehicle extends VehicleError {
    public InactiveVehicle(UUID id) {
      super(String.format("Vehicle with ID %s is inactive and cannot be modified.", id));
    }
  }

  /** Indicates a duplicate normalized license plate within the organization. */
  @Getter
  public static class PlateAlreadyExists extends VehicleError {

    private final String licensePlate;

    public PlateAlreadyExists(String licensePlate) {
      super("Vehicle plate already exists");
      this.licensePlate = licensePlate;
    }
  }

  /** Conceals absent or foreign records behind one resource-not-found failure. */
  @Getter
  public static class NotFound extends VehicleError {

    private final UUID vehicleId;

    public NotFound(UUID vehicleId) {
      super("Vehicle not found");
      this.vehicleId = vehicleId;
    }
  }

  /** Rejects vehicle deactivation while collection history references it. */
  @Getter
  public static class HasCollectionBinding extends VehicleError {

    private final UUID vehicleId;

    public HasCollectionBinding(UUID vehicleId) {
      super("The vehicle is referenced by collection history and cannot be deactivated.");
      this.vehicleId = vehicleId;
    }
  }
}
