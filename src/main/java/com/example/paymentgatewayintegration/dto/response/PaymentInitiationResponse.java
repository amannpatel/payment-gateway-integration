package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.PaymentStatus;

import java.time.OffsetDateTime;

public record PaymentInitiationResponse(
        String merchantOrderId,
        String gatewayOrderId,
        String gatewayPaymentId,
        PaymentStatus merchantVisibleStatus,
        PaymentStatus gatewayStatus,
        boolean reconciliationQueued,
        boolean webhookScheduled,
        String message,
        OffsetDateTime updatedAt
) {
}