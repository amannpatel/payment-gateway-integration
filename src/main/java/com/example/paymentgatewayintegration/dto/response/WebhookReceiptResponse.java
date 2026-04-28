package com.example.paymentgatewayintegration.dto.response;

import java.time.OffsetDateTime;

public record WebhookReceiptResponse(
        boolean accepted,
        String eventId,
        boolean duplicateDelivery,
        String processingMode,
        OffsetDateTime receivedAt
) {
}