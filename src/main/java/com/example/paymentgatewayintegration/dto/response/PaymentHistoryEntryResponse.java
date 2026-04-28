package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.PaymentEventSource;
import com.example.paymentgatewayintegration.enums.PaymentStatus;

import java.time.OffsetDateTime;

public record PaymentHistoryEntryResponse(
        Integer attemptNumber,
        PaymentStatus fromStatus,
        PaymentStatus toStatus,
        PaymentEventSource source,
        String sourceReference,
        String notes,
        OffsetDateTime createdAt
) {
}