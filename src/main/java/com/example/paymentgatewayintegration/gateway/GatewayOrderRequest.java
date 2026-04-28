package com.example.paymentgatewayintegration.gateway;

import java.math.BigDecimal;
import java.util.Map;

public record GatewayOrderRequest(
        String merchantOrderId,
        BigDecimal amount,
        String currency,
        String description,
        Map<String, String> metadata
) {
}