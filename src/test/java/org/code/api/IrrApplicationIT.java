package org.code.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies migrated application startup and the health/authentication HTTP boundary.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class IrrApplicationIT extends PostgresIntegrationTest {
  @Autowired MockMvc http;

  /**
   * Verifies the official specification exposes bearer authentication and concrete session
   * responses.
   */
  @Test
  void openApiExposesOfficialContract() throws Exception {
    http.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("IRR Backend API"))
        .andExpect(jsonPath("$.info.contact.name").value("Enzo Ribas"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
        .andExpect(jsonPath("$.paths['/api/session/authenticate'].post.security").isEmpty())
        .andExpect(jsonPath("$.components.schemas.LoginResponseDTO.properties.token").exists())
        .andExpect(jsonPath("$.paths['/api/vehicles'].get").exists())
        .andExpect(
            jsonPath("$.components.schemas.SaleResponse.properties.totalValue.type")
                .value("string"))
        .andExpect(jsonPath("$.components.schemas.BuyerRequest.properties.document").exists())
        .andExpect(jsonPath("$.components.schemas.TeamMemberRequest.properties.role").exists())
        .andExpect(
            jsonPath(
                    "$.paths['/api/v1/sales/{id}/post'].post.parameters[?(@.name=='Idempotency-Key')].required")
                .value(org.hamcrest.Matchers.contains(true)));
  }

  /** Verifies Swagger UI resources are reachable without a session token. */
  @Test
  void swaggerUiIsAvailable() throws Exception {
    http.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
  }

  /** Verifies that context loads. */
  @Test
  void contextLoads() {}

  /** Verifies that health is available without exposing details. */
  @Test
  void healthIsAvailableWithoutExposingDetails() throws Exception {
    http.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components").doesNotExist());
  }

  /** Verifies that business endpoints still require authentication. */
  @Test
  void businessEndpointsStillRequireAuthentication() throws Exception {
    http.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
  }
}
