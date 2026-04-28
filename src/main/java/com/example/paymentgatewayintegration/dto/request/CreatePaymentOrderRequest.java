package com.example.paymentgatewayintegration.dto.request;

import com.example.paymentgatewayintegration.enums.GatewayType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreatePaymentOrderRequest(
        @NotBlank String merchantId,
        @NotBlank @Size(max = 128) String merchantOrderId,
        @NotNull GatewayType gateway,
        @NotNull @DecimalMin(value = "1.00") BigDecimal amount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @Size(max = 512) String description,
        @Email String customerEmail,
        @Size(max = 32) String customerPhone,
        Map<String, String> metadata
) {
}