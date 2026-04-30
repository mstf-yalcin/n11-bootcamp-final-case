package com.n11.bootcamp.order_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("Order saga producer. Choreography saga: ORDER_CREATED -> STOCK_RESERVED -> PAYMENT_COMPLETED -> ORDER_CONFIRMED -> SHIPMENT_CREATED")
                        .version("v1.0.0"));
    }
}
