package com.example.paymentgatewayintegration.dto.request;

public record SimulateDuplicateWebhookRequest(
        String merchantOrderId,
        String paymentId,
        String eventType,
        String gatewayEventId
) {
}