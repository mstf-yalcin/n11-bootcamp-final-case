package com.n11.bootcamp.stock_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI stockServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Stock Service API")
                        .description("Stock management — quantity tracking, reservation, saga choreography consumer")
                        .version("v1.0.0"));
    }
}
