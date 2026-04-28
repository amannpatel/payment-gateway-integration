package com.example.paymentgatewayintegration.dto.request;

import com.example.paymentgatewayintegration.enums.IssueStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateIssueStatusRequest(
        @NotNull IssueStatus status,
        @Size(max = 128) String assignedTo,
        @Size(max = 2048) String resolutionNotes
) {
}