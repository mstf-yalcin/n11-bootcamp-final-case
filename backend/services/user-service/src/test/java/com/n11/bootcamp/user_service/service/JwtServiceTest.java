package com.n11.bootcamp.user_service.service;

import com.n11.bootcamp.user_service.config.JwtProperties;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.UUID;

class JwtServiceTest {

    private JwtService _jwtService;
    private JwtProperties _jwtProperties;
    private RSAPrivateKey _privateKey;
    private RSAPublicKey _publicKey;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        _privateKey = (RSAPrivateKey) pair.getPrivate();
        _publicKey = (RSAPublicKey) pair.getPublic();
        _jwtProperties = new JwtProperties(
                "n11-bootcamp",
                "n11-key-1",
                30,
                1440,
                "n11BootcampRefreshTokenSecret1234!!"
        );
        _jwtService = new JwtService(_privateKey, _publicKey, _jwtProperties);
    }

    @Test
    void testGenerateToken_ShouldReturnSignedJwtWithClaims() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        String email = "test@test.com";
        List<String> roles = List.of("USER");

        // when
        String token = _jwtService.generateToken(userId, email, roles);

        // then
        Assertions.assertNotNull(token);
        SignedJWT signed = SignedJWT.parse(token);
        JWTClaimsSet claims = signed.getJWTClaimsSet();

        Assertions.assertEquals(email, claims.getSubject());
        Assertions.assertEquals("n11-bootcamp", claims.getIssuer());
        Assertions.assertEquals(userId.toString(), claims.getStringClaim("userId"));
        Assertions.assertEquals(roles, claims.getStringListClaim("roles"));
        Assertions.assertEquals("n11-key-1", signed.getHeader().getKeyID());
    }

    @Test
    void testValidateToken_ShouldReturnTrue_WhenTokenIsValid() {
        // given
        String token = _jwtService.generateToken(UUID.randomUUID(), "test@test.com", List.of("USER"));

        // when
        boolean isValid = _jwtService.validateToken(token);

        // then
        Assertions.assertTrue(isValid);
    }

    @Test
    void testValidateToken_ShouldReturnFalse_WhenTokenIsMalformed() {
        // given
        String invalidToken = "invalid.token.here";

        // when
        boolean isValid = _jwtService.validateToken(invalidToken);

        // then
        Assertions.assertFalse(isValid);
    }

    @Test
    void testValidateToken_ShouldReturnFalse_WhenSignatureDoesNotMatch() throws Exception {
        // given
        String token = _jwtService.generateToken(UUID.randomUUID(), "test@test.com", List.of("USER"));

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair other = gen.generateKeyPair();
        JwtService otherService = new JwtService(
                (RSAPrivateKey) other.getPrivate(),
                (RSAPublicKey) other.getPublic(),
                _jwtProperties
        );

        // when
        boolean isValid = otherService.validateToken(token);

        // then
        Assertions.assertFalse(isValid);
    }
}
