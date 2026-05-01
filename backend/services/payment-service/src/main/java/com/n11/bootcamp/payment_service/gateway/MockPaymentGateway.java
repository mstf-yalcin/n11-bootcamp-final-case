package com.n11.bootcamp.payment_service.gateway;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    private static final String FAIL_TRIGGER = "mock-fail";

    @Override
    public PaymentGatewayResult charge(PaymentGatewayRequest request) {
        if (request.correlationId() != null && request.correlationId().startsWith(FAIL_TRIGGER)) {
            log.warn("Mock gateway forced FAIL via correlationId trigger: orderId={}", request.orderId());
            return PaymentGatewayResult.fail("MOCK_FORCED_FAIL", "Mock gateway forced failure for testing");
        }
        String mockId = "mock_" + UUID.randomUUID();
        log.info("Mock gateway charged: orderId={}, amount={} {}, providerPaymentId={}",
                request.orderId(), request.amount(), request.currency(), mockId);
        return PaymentGatewayResult.ok(mockId);
    }

    @Override
    public PaymentGatewayResult refund(UUID orderId, String providerPaymentId, BigDecimal amount, String correlationId) {
        log.info("Mock gateway refunded: orderId={}, providerPaymentId={}, amount={}",
                orderId, providerPaymentId, amount);
        return PaymentGatewayResult.ok(providerPaymentId);
    }
}
