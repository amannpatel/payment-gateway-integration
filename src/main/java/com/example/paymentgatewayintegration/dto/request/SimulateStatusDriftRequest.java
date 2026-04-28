package com.example.paymentgatewayintegration.dto.request;

public record SimulateStatusDriftRequest(
        String merchantOrderId,
        String paymentId,
        String note
) {
}