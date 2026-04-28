package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.PaymentStatus;

import java.time.OffsetDateTime;

public record PaymentAttemptResponse(
        Integer attemptNumber,
        String gatewayPaymentId,
        PaymentStatus status,
        String failureCode,
        String failureReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}