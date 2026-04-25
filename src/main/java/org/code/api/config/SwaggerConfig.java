package org.code.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Classe de configuração do Swagger para a documentação da API.
 * Define informações gerais da API e configurações de segurança.
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configura o OpenAPI com informações da API e esquema de segurança.
     *
     * @return uma instância configurada de {@link OpenAPI}.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("IRR API")
                        .version("0.1")
                        .description("Documentação das rotas da aplicação.")
                        .contact(new Contact().name("CODE JR").email("enzo.ribas@juniorcode.com.br")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )

                );
    }
}
