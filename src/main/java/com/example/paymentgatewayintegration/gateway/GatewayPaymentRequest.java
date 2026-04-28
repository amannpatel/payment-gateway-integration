package com.example.paymentgatewayintegration.gateway;

import com.example.paymentgatewayintegration.enums.SimulationScenario;

import java.math.BigDecimal;

public record GatewayPaymentRequest(
        String gatewayOrderId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        SimulationScenario scenario,
        long webhookDelaySeconds
) {
}