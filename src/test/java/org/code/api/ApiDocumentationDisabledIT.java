package org.code.api;

import org.code.api.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ensures disabling official documentation removes the anonymous documentation bypass.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {"springdoc.api-docs.enabled=false", "springdoc.swagger-ui.enabled=false"})
class ApiDocumentationDisabledIT extends PostgresIntegrationTest {
    @Autowired MockMvc http;
    /** Verifies documentation is inaccessible to anonymous requests when disabled. */
    @Test void documentationIsNotPublicWhenDisabled() throws Exception {
        http.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
        http.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
    }
}
