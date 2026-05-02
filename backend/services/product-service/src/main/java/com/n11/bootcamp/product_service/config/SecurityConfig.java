package com.n11.bootcamp.product_service.config;

import com.n11.bootcamp.common_lib.auth.UserPrincipalConverter;
import com.n11.bootcamp.common_lib.auth.enums.Role;
import com.n11.bootcamp.common_lib.config.SecurityPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityPaths.SWAGGER).permitAll()
                        .requestMatchers(SecurityPaths.ACTUATOR_PUBLIC).permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole(Role.ADMIN.name())

                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/products/**",
                                "/api/v1/categories/**",
                                "/api/v1/tags/**").permitAll()

                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/products/**",
                                "/api/v1/categories/**",
                                "/api/v1/tags/**").hasRole(Role.ADMIN.name())

                        .requestMatchers(HttpMethod.PUT,
                                "/api/v1/products/**",
                                "/api/v1/categories/**",
                                "/api/v1/tags/**").hasRole(Role.ADMIN.name())

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/products/**",
                                "/api/v1/categories/**",
                                "/api/v1/tags/**").hasRole(Role.ADMIN.name())

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new UserPrincipalConverter())))
                .build();
    }
}
