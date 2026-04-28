package com.example.paymentgatewayintegration.dto.response;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String correlationId
) {
}