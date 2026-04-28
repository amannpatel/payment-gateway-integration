package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.GatewayType;
import com.example.paymentgatewayintegration.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TransactionDiagnosticsResponse(
        String merchantId,
        String merchantOrderId,
        GatewayType gateway,
        String gatewayOrderId,
        PaymentStatus localOrderStatus,
        PaymentStatus gatewayObservedStatus,
        String latestGatewayPaymentId,
        BigDecimal amount,
        String currency,
        String correlationId,
        String failureReason,
        OffsetDateTime updatedAt,
        List<PaymentAttemptResponse> attempts,
        List<PaymentHistoryEntryResponse> history,
        List<WebhookEventResponse> webhookEvents
) {
}