package com.n11.bootcamp.stock_service.config;

import com.n11.bootcamp.common_lib.auth.UserPrincipalConverter;
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
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stocks/**").authenticated()
//                        .requestMatchers(HttpMethod.POST, "/api/v1/stocks/**").hasAuthority("ROLE_ADMIN")
//                        .requestMatchers(HttpMethod.PUT, "/api/v1/stocks/**").hasAuthority("ROLE_ADMIN")
//                        .requestMatchers(HttpMethod.DELETE, "/api/v1/stocks/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new UserPrincipalConverter())))
                .build();
    }
}
