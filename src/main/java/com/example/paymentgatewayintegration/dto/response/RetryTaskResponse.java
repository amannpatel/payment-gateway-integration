package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.RetryTaskStatus;
import com.example.paymentgatewayintegration.enums.RetryTaskType;

import java.time.OffsetDateTime;

public record RetryTaskResponse(
        Long id,
        RetryTaskType taskType,
        RetryTaskStatus status,
        String entityType,
        String entityId,
        int attemptCount,
        int maxAttempts,
        OffsetDateTime nextAttemptAt,
        String lastError,
        OffsetDateTime updatedAt
) {
}