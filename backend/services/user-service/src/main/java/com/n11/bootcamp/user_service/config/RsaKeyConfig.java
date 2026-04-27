package com.n11.bootcamp.user_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
public class RsaKeyConfig {

    @Bean
    public RSAPrivateKey rsaPrivateKey(@Value("classpath:certs/private.pem") Resource resource) throws Exception {
        return RsaKeyConverters.pkcs8().convert(resource.getInputStream());
    }

    @Bean
    public RSAPublicKey rsaPublicKey(@Value("classpath:certs/public.pem") Resource resource) throws Exception {
        return RsaKeyConverters.x509().convert(resource.getInputStream());
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }
}
