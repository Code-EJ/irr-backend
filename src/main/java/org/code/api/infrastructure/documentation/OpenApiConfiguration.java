package org.code.api.infrastructure.documentation;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Configures Swagger/OpenAPI as the official executable HTTP contract reference.
 * It documents current declarations without replacing runtime authorization checks.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Configuration
public class OpenApiConfiguration {
    /**
     * Describes the API and its bearer authentication mechanism.
     * @return the root OpenAPI document metadata
     */
    @Bean
    public OpenAPI irrOpenApi() {
        return new OpenAPI().info(new Info().title("IRR Backend API").version("0.0.1-SNAPSHOT")
            .description("Pre-production waste management API. This specification documents the current contract; "
                + "role, ownership and inventory redesign work is tracked in the repository ADRs.")
            .contact(new Contact().name("Enzo Ribas").url("https://github.com/oEnzoRibas")))
            .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * Enriches generated operations with English summaries and declared access rules.
     * @return an operation customizer applied to controller declarations
     */
    @Bean
    public OperationCustomizer operationDocumentation() {
        Map<String, String> resources = Map.ofEntries(
            Map.entry("SessionController", "Sessions"), Map.entry("VehicleController", "Vehicles"),
            Map.entry("MaterialCategoryController", "Material categories"), Map.entry("MaterialTypeController", "Material types"),
            Map.entry("MaterialSubtypeController", "Material subtypes"), Map.entry("DonorController", "Donors"),
            Map.entry("DonationController", "Donations"), Map.entry("SortingController", "Sorting"),
            Map.entry("PressingController", "Pressing"), Map.entry("DocumentController", "Attachments"));
        Map<String, String> actions = Map.ofEntries(
            Map.entry("register", "Register a user"), Map.entry("authenticate", "Authenticate a user"),
            Map.entry("create", "Create"), Map.entry("list", "List"), Map.entry("getById", "Get by ID"),
            Map.entry("update", "Update"), Map.entry("deactivate", "Deactivate"),
            Map.entry("bulkCreate", "Create a batch"), Map.entry("bulkUpdate", "Update a batch"),
            Map.entry("uploadDocumento", "Upload an attachment"), Map.entry("downloadDocumento", "Download an attachment"),
            Map.entry("deletarDocumento", "Delete an attachment"));
        return (operation, handler) -> {
            String resource = resources.getOrDefault(handler.getBeanType().getSimpleName(), "Operations");
            String method = handler.getMethod().getName();
            operation.setTags(List.of(resource));
            operation.setSummary(actions.getOrDefault(method, "Process request") + " — " + resource);
            PreAuthorize rule = handler.getMethodAnnotation(PreAuthorize.class);
            operation.setDescription(rule == null
                ? "Access follows the session filter and application service. See the master plan for pending authorization changes."
                : "Declared method authorization: " + rule.value() + ". Object ownership is enforced by the corresponding service where implemented.");
            return operation;
        };
    }

    /**
     * Documents public session operations as exceptions to the global bearer requirement.
     * @return a customizer for the two existing public session routes
     */
    @Bean
    public OpenApiCustomizer sessionSecurityDocumentation() {
        return api -> {
            if (api.getPaths() == null) return;
            for (String path : List.of("/api/session/register", "/api/session/authenticate")) {
                var item = api.getPaths().get(path);
                if (item != null) item.readOperations().forEach(operation -> operation.setSecurity(List.of()));
            }
        };
    }
}
