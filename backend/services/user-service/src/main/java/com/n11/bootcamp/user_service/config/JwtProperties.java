package com.n11.bootcamp.user_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String issuer,
        String keyId,
        long expireMinutes,
        long refreshExpireMinutes,
        String hmacSecret) {
}
