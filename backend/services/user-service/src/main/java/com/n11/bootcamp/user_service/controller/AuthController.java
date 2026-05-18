package com.n11.bootcamp.user_service.controller;

import com.n11.bootcamp.common_lib.auth.UserPrincipal;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.user_service.dto.request.AuthRequest;
import com.n11.bootcamp.user_service.dto.request.RefreshTokenRequest;
import com.n11.bootcamp.user_service.dto.request.RegisterRequest;
import com.n11.bootcamp.user_service.dto.request.ValidateTokenRequest;
import com.n11.bootcamp.user_service.dto.response.AccessTokenResponse;
import com.n11.bootcamp.user_service.dto.response.TokenResponse;
import com.n11.bootcamp.user_service.dto.response.UserResponse;
import com.n11.bootcamp.user_service.exception.InvalidTokenException;
import com.n11.bootcamp.user_service.service.AuthService;
import com.n11.bootcamp.user_service.util.CookieUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    public AuthController(AuthService authService, CookieUtil cookieUtil) {
        this.authService = authService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> register(
            @Valid @RequestBody RegisterRequest authRequest) {
        TokenResponse tokens = authService.register(authRequest);
        ResponseCookie refreshCookie = cookieUtil.create(tokens.refreshToken());
        return ResponseEntity
                .created(URI.create("/api/v1/test/user"))
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(
                        new AccessTokenResponse(tokens.accessToken()),
                        "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> login(
            @Valid @RequestBody AuthRequest authRequest) {
        TokenResponse tokens = authService.login(authRequest);
        ResponseCookie refreshCookie = cookieUtil.create(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(
                        new AccessTokenResponse(tokens.accessToken()),
                        "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refreshToken(
            @CookieValue(name = "${app.cookie.refresh-token-name:refreshToken}", required = false) String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidTokenException("Refresh token cookie missing");
        }

        TokenResponse tokens = authService.refreshToken(new RefreshTokenRequest(refreshToken));
        ResponseCookie refreshCookie = cookieUtil.create(tokens.refreshToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success(
                        new AccessTokenResponse(tokens.accessToken()),
                        "Token refreshed successfully"));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<Boolean>> validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.validate(request.token()), "Token is valid"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "${app.cookie.refresh-token-name:refreshToken}", required = false) String refreshToken) {

        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(new RefreshTokenRequest(refreshToken), null);
        }

        ResponseCookie clearCookie = cookieUtil.clear();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfo(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(authService.getUserInfo(principal.email()), "User information retrieved"));
    }
}
