package com.n11.bootcamp.common_lib.config;

public final class SecurityPaths {

    public static final String[] SWAGGER = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs*/**"
    };

    public static final String[] ACTUATOR_PUBLIC = {
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus"
    };

    private SecurityPaths() {
    }
}
