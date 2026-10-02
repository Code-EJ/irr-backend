package org.code.api.dto.logistic.vehicle.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Validated vehicle bulk create request contract fields for the organization-scoped API.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public record VehicleBulkCreateRequestDTO(
    @NotEmpty(message = "Vehicle list must not be empty")
        @Size(max = 100, message = "Cannot create more than 100 vehicles at once")
        @Valid
        List<VehicleCreateRequestDTO> vehicles) {}
