package com.example.paymentgatewayintegration.dto.response;

import com.example.paymentgatewayintegration.enums.IssueStatus;
import com.example.paymentgatewayintegration.enums.IssueType;

import java.time.OffsetDateTime;

public record MerchantIssueResponse(
        Long id,
        String merchantId,
        String merchantOrderId,
        String paymentId,
        IssueType issueType,
        IssueStatus status,
        String summary,
        String description,
        String assignedTo,
        String resolutionNotes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}