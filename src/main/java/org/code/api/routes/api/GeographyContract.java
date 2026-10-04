package org.code.api.routes.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Typed shared territorial references; code shape validation does not certify an official dataset
 * entry.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
public final class GeographyContract {
  private GeographyContract() {}

  /** Registers a reviewed Brazilian municipality without importing a dataset. */
  @Schema(name = "MunicipalityRequest")
  public record MunicipalityRequest(
      @NotBlank @Pattern(regexp = "[0-9]{7}") String ibgeCode,
      @NotBlank @Size(max = 150) String name,
      @NotBlank
          @Pattern(
              regexp =
                  "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO")
          String stateCode) {}

  /** Returns stable municipality identity and display metadata. */
  @Schema(name = "MunicipalityResponse")
  public record MunicipalityResponse(
      UUID id, String ibgeCode, String name, String stateCode, OffsetDateTime createdAt) {}

  /** Registers one stable named neighborhood within a reviewed municipality. */
  @Schema(name = "NeighborhoodRequest")
  public record NeighborhoodRequest(
      @NotNull UUID municipalityId, @NotBlank @Size(max = 150) String name) {}

  /** Identifies a declared neighborhood without claiming polygon or GPS data. */
  @Schema(name = "NeighborhoodResponse")
  public record NeighborhoodResponse(
      UUID id, UUID municipalityId, String name, OffsetDateTime createdAt) {}
}
