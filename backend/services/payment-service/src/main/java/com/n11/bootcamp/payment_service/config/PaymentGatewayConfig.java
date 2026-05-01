package com.n11.bootcamp.payment_service.config;

import com.iyzipay.Options;
import com.n11.bootcamp.payment_service.gateway.IyzicoPaymentGateway;
import com.n11.bootcamp.payment_service.gateway.MockPaymentGateway;
import com.n11.bootcamp.payment_service.gateway.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(IyzicoProperties.class)
public class PaymentGatewayConfig {

    private static final String MODE_SANDBOX = "sandbox";

    @Bean
    public Options iyzicoOptions(IyzicoProperties props) {
        Options options = new Options();
        options.setApiKey(props.iyzico().apiKey());
        options.setSecretKey(props.iyzico().secretKey());
        options.setBaseUrl(props.iyzico().baseUrl());
        return options;
    }

    @Bean
    public PaymentGateway paymentGateway(IyzicoProperties props, Options iyzicoOptions) {
        if (MODE_SANDBOX.equalsIgnoreCase(props.mode())) {
            log.info("Payment gateway initialized: provider=IYZICO, mode=sandbox, baseUrl={}",
                    props.iyzico().baseUrl());
            return new IyzicoPaymentGateway(iyzicoOptions, props.iyzico().testCard());
        }
        log.warn("Payment gateway initialized in MOCK mode — all charges auto-succeed");
        return new MockPaymentGateway();
    }
}
