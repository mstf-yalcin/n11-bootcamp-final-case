package com.n11.bootcamp.payment_service.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    PaymentGatewayResult charge(PaymentGatewayRequest request);

    PaymentGatewayResult refund(UUID orderId, String providerPaymentId, BigDecimal amount, String correlationId);
}
