package com.n11.bootcamp.common_lib.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-key";

    @Bean
    public OpenAPI commonOpenAPI(
            @Value("${spring.application.name:service}") String appName,
            @Value("${app.openapi.gateway-url:http://localhost:8080}") String gatewayUrl) {
        return new OpenAPI()
                .servers(List.of(new Server().url(gatewayUrl).description("API Gateway")))
                .info(new Info()
                        .title("N11 Bootcamp " + appName.toUpperCase() + " API")
                        .description("N11 Bootcamp microservices project — " + appName)
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Mustafa")
                                .email("test@gmail.com")
                                .url("https://test.com")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)));
    }
}
