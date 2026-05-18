package com.n11.bootcamp.user_service.service;

import com.n11.bootcamp.user_service.config.JwtProperties;
import com.n11.bootcamp.user_service.exception.TokenGenerationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final JwtProperties jwt;

    public JwtService(RSAPrivateKey privateKey,
                      RSAPublicKey publicKey,
                      JwtProperties jwt) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.jwt = jwt;
    }

    public String generateToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(jwt.expireMinutes(), ChronoUnit.MINUTES);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(email)
                .issuer(jwt.issuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("userId", userId.toString())
                .claim("roles", roles)
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(jwt.keyId()).build(),
                claims
        );

        try {
            signedJWT.sign(new RSASSASigner(privateKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new TokenGenerationException(e);
        }
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            return signedJWT.verify(new RSASSAVerifier(publicKey))
                    && jwt.issuer().equals(claims.getIssuer())
                    && claims.getExpirationTime().toInstant().isAfter(Instant.now());
        } catch (ParseException | JOSEException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
