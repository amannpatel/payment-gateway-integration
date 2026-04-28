package com.example.paymentgatewayintegration.gateway;

public record GatewayOrderResponse(
        String gatewayOrderId,
        String rawResponse
) {
}