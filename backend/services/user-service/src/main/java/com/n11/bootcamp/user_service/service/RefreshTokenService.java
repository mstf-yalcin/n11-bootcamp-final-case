package com.n11.bootcamp.user_service.service;

import com.n11.bootcamp.user_service.config.JwtProperties;
import com.n11.bootcamp.user_service.dto.internal.RefreshTokenDto;
import com.n11.bootcamp.user_service.dto.request.RefreshTokenRequest;
import com.n11.bootcamp.user_service.entity.RefreshToken;
import com.n11.bootcamp.user_service.entity.User;
import com.n11.bootcamp.user_service.exception.InvalidTokenException;
import com.n11.bootcamp.user_service.repository.RefreshTokenRepository;
import com.n11.bootcamp.user_service.util.TokenHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Slf4j
@Service
public class RefreshTokenService {

    private final JwtProperties jwt;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;

    public RefreshTokenService(JwtProperties jwt,
                               RefreshTokenRepository refreshTokenRepository,
                               TokenHasher tokenHasher) {
        this.jwt = jwt;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public String generateRefreshToken(User user) {
        log.debug("Generating new refresh token for user: {}", user.getUsername());

        String rawToken = generateSecureToken();
        String hashedToken = tokenHasher.hash(rawToken);

        var refreshTokenEntity = RefreshToken.builder()
                .token(hashedToken)
                .expiration(Instant.now().plus(jwt.refreshExpireMinutes(), ChronoUnit.MINUTES))
                .user(user)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
        return rawToken;
    }

    @Transactional
    public RefreshTokenDto refreshTokenLogin(RefreshTokenRequest refreshTokenRequestDto) {
        String hashedToken = tokenHasher.hash(refreshTokenRequestDto.refreshToken());

        var token = refreshTokenRepository.findByToken(hashedToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (token.isRevoked()) {
            log.warn("Token reuse for user {}! This session has been revoked.", token.getUser().getUsername());
            throw new InvalidTokenException("Token reuse detected. This session has been revoked. Please make a new login request.");
        }

        if (isTokenExpired(token.getExpiration())) {
            log.info("Refresh token expired for user: {}", token.getUser().getUsername());
            refreshTokenRepository.delete(token);
            throw new InvalidTokenException("Refresh token is expired. Please make a new login request.");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = token.getUser();
        String newRawToken = generateRefreshToken(user);
        return new RefreshTokenDto(newRawToken, user);
    }

    @Transactional
    public void revokeToken(String rawToken) {
        String hashedToken = tokenHasher.hash(rawToken);
        var tokenOpt = refreshTokenRepository.findByToken(hashedToken);

        if (tokenOpt.isPresent()) {
            var token = tokenOpt.get();
            log.debug("Revoking refresh token for user: {}", token.getUser().getUsername());
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }

    }

    private boolean isTokenExpired(Instant expirationTime) {
        return expirationTime.isBefore(Instant.now());
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
