package com.example.paymentgatewayintegration.gateway;

import com.example.paymentgatewayintegration.enums.PaymentStatus;

public record GatewayPaymentResponse(
        String gatewayPaymentId,
        PaymentStatus status,
        PaymentStatus gatewayFinalStatus,
        String authorizationId,
        String gatewayReference,
        String rawResponse,
        boolean timedOut,
        boolean merchantViewShouldFail,
        boolean shouldScheduleWebhook,
        String webhookEventType,
        long webhookDelaySeconds,
        String failureCode,
        String failureReason
) {
}