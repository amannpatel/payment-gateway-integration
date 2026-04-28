package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.GatewayType;
import com.example.paymentgatewayintegration.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PaymentStatusResponse(
        String merchantId,
        String merchantOrderId,
        GatewayType gateway,
        String gatewayOrderId,
        String gatewayPaymentId,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        String failureReason,
        OffsetDateTime updatedAt,
        List<PaymentAttemptResponse> attempts,
        List<PaymentHistoryEntryResponse> history
) {
}