package org.code.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Prevents transitional application routes from reentering the supported frontend contract.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
class VersionedApiContractIT extends PostgresIntegrationTest {
  @Autowired MockMvc http;
  @Autowired ObjectMapper json;

  @Autowired
  @Qualifier("requestMappingHandlerMapping")
  RequestMappingHandlerMapping mappings;

  /** Both actual application handlers and Swagger expose only the supported version. */
  @Test
  void everyApplicationRouteUsesV1WithoutDeprecatedOperations() throws Exception {
    var paths =
        mappings.getHandlerMethods().entrySet().stream()
            .filter(
                entry -> entry.getValue().getBeanType().getPackageName().startsWith("org.code.api"))
            .flatMap(entry -> entry.getKey().getPatternValues().stream())
            .toList();
    assertThat(paths).isNotEmpty().allMatch(path -> path.startsWith("/api/v1/"));
    assertThat(paths).doesNotContain("/api/v1/session/register");
    var specification =
        json.readTree(
            http.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    specification
        .get("paths")
        .fields()
        .forEachRemaining(
            entry -> {
              assertThat(entry.getKey()).startsWith("/api/v1/");
              entry
                  .getValue()
                  .forEach(
                      operation -> assertThat(operation.path("deprecated").asBoolean()).isFalse());
            });
  }

  /** Authenticated callers receive no alias or redirect; anonymous callers remain protected. */
  @Test
  void removedRoutesCannotBeUsed() throws Exception {
    for (String path :
        List.of(
            "/api/vehicles",
            "/api/documents",
            "/api/donors",
            "/api/donations",
            "/api/materials/categories",
            "/api/materials/types",
            "/api/materials/subtypes",
            "/api/pressings",
            "/api/sortings",
            "/api/users",
            "/api/organizations")) {
      http.perform(get(path).with(user("contract-check"))).andExpect(status().isNotFound());
    }
    for (String path :
        List.of("/api/session/authenticate", "/api/session/register", "/api/v1/session/register")) {
      http.perform(post(path).with(user("contract-check"))).andExpect(status().isNotFound());
      http.perform(post(path)).andExpect(status().isUnauthorized());
    }
  }
}
