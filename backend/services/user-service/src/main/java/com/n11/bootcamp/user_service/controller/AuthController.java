package com.n11.bootcamp.user_service.controller;

import com.n11.bootcamp.common_lib.auth.UserPrincipal;
import com.n11.bootcamp.user_service.dto.request.AuthRequest;
import com.n11.bootcamp.user_service.dto.request.RefreshTokenRequest;
import com.n11.bootcamp.user_service.dto.request.RegisterRequest;
import com.n11.bootcamp.user_service.dto.request.ValidateTokenRequest;
import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.user_service.dto.response.TokenResponse;
import com.n11.bootcamp.user_service.dto.response.UserResponse;
import com.n11.bootcamp.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest authRequest) {
        return ResponseEntity
                .created(URI.create("/api/v1/test/user"))
                .body(ApiResponse.success(authService.register(authRequest), "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody AuthRequest authRequest) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(authRequest), "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity
                .ok(ApiResponse.success(authService.refreshToken(refreshTokenRequest), "Token refreshed successfully"));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<Boolean>> validate(@Valid @RequestBody ValidateTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.validate(request.token()), "Token is valid"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(request, authHeader);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getUserInfo(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(authService.getUserInfo(principal.email()), "User information retrieved"));
    }
}
