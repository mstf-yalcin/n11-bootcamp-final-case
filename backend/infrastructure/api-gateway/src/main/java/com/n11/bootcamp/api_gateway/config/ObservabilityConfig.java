package com.n11.bootcamp.api_gateway.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class ObservabilityConfig {

    @Bean
    public ObservationPredicate noActuatorObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext reactiveContext) {
                String path = reactiveContext.getCarrier().getPath().pathWithinApplication().value();
                return path == null || !path.startsWith("/actuator");
            }
            return true;
        };
    }
}
