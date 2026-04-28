package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.GatewayType;
import com.example.paymentgatewayintegration.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentOrderResponse(
        String merchantId,
        String merchantOrderId,
        GatewayType gateway,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String gatewayOrderId,
        String latestPaymentId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}