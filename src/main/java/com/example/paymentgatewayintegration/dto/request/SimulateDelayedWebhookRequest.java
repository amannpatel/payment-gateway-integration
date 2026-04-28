package com.example.paymentgatewayintegration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SimulateDelayedWebhookRequest(
        String merchantOrderId,
        String paymentId,
        @NotBlank String eventType,
        @Positive long delaySeconds
) {
}