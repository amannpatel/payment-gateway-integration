package com.example.paymentgatewayintegration.dto.request;

import com.example.paymentgatewayintegration.enums.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(
        @NotBlank String merchantId,
        String merchantOrderId,
        String paymentId,
        @NotNull IssueType issueType,
        @NotBlank @Size(max = 255) String summary,
        @NotBlank @Size(max = 2048) String description
) {
}