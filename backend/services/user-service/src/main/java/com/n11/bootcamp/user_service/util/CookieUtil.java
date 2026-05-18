package com.n11.bootcamp.user_service.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    private final boolean secure;
    private final String sameSite;
    private final String path;
    private final long maxAge;
    private final String refreshTokenName;

    public CookieUtil(
            @Value("${app.cookie.secure:false}") boolean secure,
            @Value("${app.cookie.same-site:Strict}") String sameSite,
            @Value("${app.cookie.path:/api/v1/auth}") String path,
            @Value("${app.cookie.max-age:604800}") long maxAge,
            @Value("${app.cookie.refresh-token-name:refreshToken}") String refreshTokenName) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.path = path;
        this.maxAge = maxAge;
        this.refreshTokenName = refreshTokenName;
    }

    public String refreshTokenName() {
        return refreshTokenName;
    }

    public ResponseCookie create(String value) {
        return ResponseCookie.from(refreshTokenName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(maxAge))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(refreshTokenName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(0)
                .build();
    }
}
