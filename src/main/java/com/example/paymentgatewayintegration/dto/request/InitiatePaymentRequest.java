package com.example.paymentgatewayintegration.dto.request;

import com.example.paymentgatewayintegration.enums.SimulationScenario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record InitiatePaymentRequest(
        @NotBlank String paymentMethod,
        SimulationScenario scenario,
        @PositiveOrZero Long webhookDelaySeconds
) {
}