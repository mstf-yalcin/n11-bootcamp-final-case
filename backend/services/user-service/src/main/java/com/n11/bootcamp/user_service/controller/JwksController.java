package com.n11.bootcamp.user_service.controller;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

@RestController
public class JwksController {

    private final RSAPublicKey publicKey;
    private final String keyId;

    public JwksController(RSAPublicKey publicKey, @Value("${jwt.key-id}") String keyId) {
        this.publicKey = publicKey;
        this.keyId = keyId;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .keyID(keyId)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(new Algorithm("RS256") {})
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }
}
