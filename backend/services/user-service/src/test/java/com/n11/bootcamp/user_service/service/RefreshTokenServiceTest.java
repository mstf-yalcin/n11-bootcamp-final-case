package com.n11.bootcamp.user_service.service;

import com.n11.bootcamp.user_service.config.JwtProperties;
import com.n11.bootcamp.user_service.dto.internal.RefreshTokenDto;
import com.n11.bootcamp.user_service.dto.request.RefreshTokenRequest;
import com.n11.bootcamp.user_service.entity.RefreshToken;
import com.n11.bootcamp.user_service.entity.User;
import com.n11.bootcamp.user_service.exception.InvalidTokenException;
import com.n11.bootcamp.user_service.repository.RefreshTokenRepository;
import com.n11.bootcamp.user_service.util.TokenHasher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private RefreshTokenService _refreshTokenService;

    @Mock
    private RefreshTokenRepository _refreshTokenRepository;

    @Mock
    private TokenHasher _tokenHasher;

    private final JwtProperties _jwtProperties = new JwtProperties(
            "n11-bootcamp",
            "n11-key-1",
            30,
            1440,
            "n11BootcampRefreshTokenSecret1234!!"
    );

    @BeforeEach
    void setUp() {
        _refreshTokenService = new RefreshTokenService(_jwtProperties, _refreshTokenRepository, _tokenHasher);
    }

    @Test
    void testGenerateRefreshToken_ShouldCreateAndSaveNewTokenAndReturnRawString() {
        // given
        User user = new User();
        user.setEmail("testuser@test.com");

        Mockito.when(_tokenHasher.hash(Mockito.anyString())).thenReturn("hashed-token");
        Mockito.when(_refreshTokenRepository.save(Mockito.any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        String result = _refreshTokenService.generateRefreshToken(user);

        // then
        Assertions.assertNotNull(result);
        Mockito.verify(_tokenHasher).hash(result);
        Mockito.verify(_refreshTokenRepository).save(Mockito.any(RefreshToken.class));
    }

    @Test
    void testRefreshTokenLogin_ShouldReturnDto_WhenTokenIsValid() {
        // given
        String rawToken = "valid-token";
        String hashedToken = "hashed-valid-token";
        User user = new User();
        user.setEmail("testuser@test.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(hashedToken);
        refreshToken.setRevoked(false);
        refreshToken.setUser(user);
        refreshToken.setExpiration(Instant.now().plus(1, ChronoUnit.HOURS));

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);

        Mockito.when(_tokenHasher.hash(Mockito.anyString())).thenReturn(hashedToken, "new-hashed-token");
        Mockito.when(_refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(refreshToken));
        Mockito.when(_refreshTokenRepository.save(Mockito.any(RefreshToken.class))).thenReturn(refreshToken);

        // when
        RefreshTokenDto result = _refreshTokenService.refreshTokenLogin(request);

        // then
        Assertions.assertNotNull(result);
        Assertions.assertEquals(user, result.user());
        Assertions.assertNotNull(result.rawToken());
        Assertions.assertTrue(refreshToken.isRevoked());
        Mockito.verify(_refreshTokenRepository).save(refreshToken);
    }

    @Test
    void testRefreshTokenLogin_ShouldThrowException_WhenTokenIsRevoked() {
        // given
        String rawToken = "revoked-token";
        String hashedToken = "hashed-revoked-token";
        User user = new User();
        user.setEmail("testuser@test.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(hashedToken);
        refreshToken.setRevoked(true);
        refreshToken.setUser(user);

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);

        Mockito.when(_tokenHasher.hash(rawToken)).thenReturn(hashedToken);
        Mockito.when(_refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        // when & then
        Assertions.assertThrows(InvalidTokenException.class, () -> _refreshTokenService.refreshTokenLogin(request));
    }

    @Test
    void testRefreshTokenLogin_ShouldThrowException_WhenTokenIsExpired() {
        // given
        String rawToken = "expired-token";
        String hashedToken = "hashed-expired-token";
        User user = new User();
        user.setEmail("testuser@test.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(hashedToken);
        refreshToken.setRevoked(false);
        refreshToken.setUser(user);
        refreshToken.setExpiration(Instant.now().minus(1, ChronoUnit.MINUTES));

        RefreshTokenRequest request = new RefreshTokenRequest(rawToken);

        Mockito.when(_tokenHasher.hash(rawToken)).thenReturn(hashedToken);
        Mockito.when(_refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        // when & then
        Assertions.assertThrows(InvalidTokenException.class, () -> _refreshTokenService.refreshTokenLogin(request));
        Mockito.verify(_refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void testRevokeToken_ShouldMarkAsRevoked() {
        // given
        String rawToken = "token-to-revoke";
        String hashedToken = "hashed-token";
        User user = new User();
        user.setEmail("testuser@test.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(hashedToken);
        refreshToken.setRevoked(false);
        refreshToken.setUser(user);

        Mockito.when(_tokenHasher.hash(rawToken)).thenReturn(hashedToken);
        Mockito.when(_refreshTokenRepository.findByToken(hashedToken)).thenReturn(Optional.of(refreshToken));

        // when
        _refreshTokenService.revokeToken(rawToken);

        // then
        Assertions.assertTrue(refreshToken.isRevoked());
        Mockito.verify(_refreshTokenRepository).save(refreshToken);
    }
}
