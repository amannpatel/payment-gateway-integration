package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.GatewayType;
import com.example.paymentgatewayintegration.enums.WebhookProcessingStatus;

import java.time.OffsetDateTime;

public record WebhookEventResponse(
        Long id,
        GatewayType gateway,
        String gatewayEventId,
        String deliveryId,
        String eventType,
        boolean signatureValid,
        boolean duplicateDelivery,
        WebhookProcessingStatus processingStatus,
        String paymentId,
        String orderId,
        String processingError,
        OffsetDateTime receivedAt,
        OffsetDateTime processedAt
) {
}