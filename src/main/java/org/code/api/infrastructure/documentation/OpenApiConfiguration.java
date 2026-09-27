package org.code.api.infrastructure.documentation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Configures Swagger/OpenAPI as the official executable HTTP contract reference. It documents
 * current declarations without replacing runtime authorization checks.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Configuration
public class OpenApiConfiguration {
  /**
   * Describes the API and its bearer authentication mechanism.
   *
   * @return the root OpenAPI document metadata
   */
  @Bean
  public OpenAPI irrOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("IRR Backend API")
                .version("0.0.1-SNAPSHOT")
                .description(
                    "Pre-production waste management API. This specification documents the current"
                        + " contract; business operations require explicit organization membership."
                        + " Decimal quantities and money are JSON strings. Posted operations are"
                        + " corrected through audited reversals.")
                .contact(new Contact().name("Enzo Ribas").url("https://github.com/oEnzoRibas")))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  /**
   * Enriches generated operations with English summaries and declared access rules.
   *
   * @return an operation customizer applied to controller declarations
   */
  @Bean
  public OperationCustomizer operationDocumentation() {
    Map<String, String> resources =
        Map.ofEntries(
            Map.entry("BuyerController", "Buyers"),
            Map.entry("TeamMemberController", "Team members"),
            Map.entry("CollectionController", "Collections"),
            Map.entry("SaleController", "Sales"),
            Map.entry("InventoryController", "Inventory"),
            Map.entry("ProcessingReversalController", "Processing reversals"),
            Map.entry("ReportController", "Reports"),
            Map.entry("OrganizationController", "Organizations"),
            Map.entry("UserController", "Partners"),
            Map.entry("SessionController", "Sessions"),
            Map.entry("VehicleController", "Vehicles"),
            Map.entry("MaterialCategoryController", "Material categories"),
            Map.entry("MaterialTypeController", "Material types"),
            Map.entry("MaterialSubtypeController", "Material subtypes"),
            Map.entry("DonorController", "Donors"),
            Map.entry("DonationController", "Donations"),
            Map.entry("SortingController", "Sorting"),
            Map.entry("PressingController", "Pressing"),
            Map.entry("DocumentController", "Attachments"));
    Map<String, String> actions =
        Map.ofEntries(
            Map.entry("register", "Register a user"),
            Map.entry("authenticate", "Authenticate a user"),
            Map.entry("create", "Create"),
            Map.entry("list", "List"),
            Map.entry("getById", "Get by ID"),
            Map.entry("update", "Update"),
            Map.entry("deactivate", "Deactivate"),
            Map.entry("bulkCreate", "Create a batch"),
            Map.entry("bulkUpdate", "Update a batch"),
            Map.entry("uploadDocumento", "Upload an attachment"),
            Map.entry("downloadDocumento", "Download an attachment"),
            Map.entry("deletarDocumento", "Delete an attachment"));
    return (operation, handler) -> {
      String resource = resources.getOrDefault(handler.getBeanType().getSimpleName(), "Operations");
      String method = handler.getMethod().getName();
      operation.setTags(List.of(resource));
      if (!List.of("SessionController", "UserController", "OrganizationController")
          .contains(handler.getBeanType().getSimpleName())) {
        operation.addParametersItem(
            new Parameter()
                .name("X-Organization-Id")
                .in("header")
                .required(true)
                .description(
                    "Explicit active organization membership; platform administrator status is not"
                        + " a bypass")
                .schema(new StringSchema().format("uuid")));
      }
      String controller = handler.getBeanType().getSimpleName();
      if ((List.of("SortingController", "PressingController").contains(controller)
              && method.equals("create"))
          || (controller.equals("SaleController") && List.of("post", "reverse").contains(method))
          || controller.equals("ProcessingReversalController")) {
        operation.addParametersItem(
            new Parameter()
                .name("Idempotency-Key")
                .in("header")
                .required(true)
                .description(
                    "Organization-scoped command key. Repeat with the same payload to replay the"
                        + " committed response; changed payload returns 409.")
                .schema(new StringSchema().minLength(1).maxLength(128)));
      }
      if (operation.getSummary() == null)
        operation.setSummary(actions.getOrDefault(method, "Process request") + " — " + resource);
      PreAuthorize rule = handler.getMethodAnnotation(PreAuthorize.class);
      if (operation.getDescription() == null)
        operation.setDescription(
            rule == null
                ? "Authenticated access is checked by the application service. Business records"
                    + " require current organization membership; platform administration uses the"
                    + " administrator role."
                : "Declared method authorization: "
                    + rule.value()
                    + ". Organization ownership is checked by the application service.");
      return operation;
    };
  }

  /**
   * Documents public session operations as exceptions to the global bearer requirement.
   *
   * @return a customizer for the public login route
   */
  @Bean
  public OpenApiCustomizer sessionSecurityDocumentation() {
    return api -> {
      if (api.getPaths() == null) return;
      for (String path : List.of("/api/v1/session/authenticate")) {
        var item = api.getPaths().get(path);
        if (item != null)
          item.readOperations().forEach(operation -> operation.setSecurity(List.of()));
      }
    };
  }

  /** Serializes decimal business values as strings to prevent JavaScript precision loss. */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer decimalWireFormat() {
    SpringDocUtils.getConfig()
        .replaceWithSchema(
            BigDecimal.class,
            new StringSchema()
                .pattern("^-?[0-9]+(?:[.][0-9]+)?$")
                .example("123.4500")
                .description(
                    "Exact decimal string; use decimal arithmetic and preserve the documented"
                        + " unit"));
    return builder ->
        builder.serializerByType(
            BigDecimal.class,
            new JsonSerializer<BigDecimal>() {
              @Override
              public void serialize(
                  BigDecimal value, JsonGenerator generator, SerializerProvider provider)
                  throws IOException {
                generator.writeString(value.toPlainString());
              }
            });
  }
}
