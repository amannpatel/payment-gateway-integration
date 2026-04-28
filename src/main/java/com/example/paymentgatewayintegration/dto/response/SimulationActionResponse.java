package com.example.paymentgatewayintegration.dto.response;

import java.time.OffsetDateTime;

public record SimulationActionResponse(
        String action,
        String status,
        String message,
        String gatewayEventId,
        OffsetDateTime effectiveAt
) {
}