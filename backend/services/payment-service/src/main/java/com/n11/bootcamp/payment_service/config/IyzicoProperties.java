package com.n11.bootcamp.payment_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment")
public record IyzicoProperties(
        String mode,
        Iyzico iyzico) {

    public record Iyzico(
            String apiKey,
            String secretKey,
            String baseUrl,
            TestCard testCard) {
    }

    public record TestCard(
            String holderName,
            String number,
            String expireMonth,
            String expireYear,
            String cvc) {
    }
}
