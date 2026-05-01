package com.n11.bootcamp.payment_service.gateway;

public record PaymentGatewayResult(
        boolean success,
        String providerPaymentId,
        String errorCode,
        String errorMessage
) {
    public static PaymentGatewayResult ok(String providerPaymentId) {
        return new PaymentGatewayResult(true, providerPaymentId, null, null);
    }

    public static PaymentGatewayResult fail(String errorCode, String errorMessage) {
        return new PaymentGatewayResult(false, null, errorCode, errorMessage);
    }
}
