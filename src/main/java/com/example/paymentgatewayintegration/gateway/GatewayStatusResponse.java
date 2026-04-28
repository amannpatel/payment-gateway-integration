package com.example.paymentgatewayintegration.gateway;

import com.example.paymentgatewayintegration.enums.PaymentStatus;

public record GatewayStatusResponse(
        String gatewayPaymentId,
        PaymentStatus status,
        String rawResponse,
        String failureCode,
        String failureReason
) {
}