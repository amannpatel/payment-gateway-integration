package com.example.paymentgatewayintegration.controller;

import com.example.paymentgatewayintegration.dto.request.CreateIssueRequest;
import com.example.paymentgatewayintegration.dto.request.UpdateIssueStatusRequest;
import com.example.paymentgatewayintegration.dto.response.MerchantIssueResponse;
import com.example.paymentgatewayintegration.enums.IssueStatus;
import com.example.paymentgatewayintegration.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/support/issues")
public class SupportIssueController {

    private final IssueService issueService;

    public SupportIssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MerchantIssueResponse createIssue(@Valid @RequestBody CreateIssueRequest request) {
        return issueService.createIssue(request);
    }

    @PatchMapping("/{issueId}/status")
    public MerchantIssueResponse updateIssueStatus(@PathVariable Long issueId, @Valid @RequestBody UpdateIssueStatusRequest request) {
        return issueService.updateIssueStatus(issueId, request);
    }

    @GetMapping
    public List<MerchantIssueResponse> listIssues(@RequestParam(required = false) IssueStatus status) {
        return issueService.listIssues(status);
    }

    @GetMapping("/export/daily")
    public ResponseEntity<byte[]> exportDailyIssues(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        byte[] file = issueService.exportDailyIssues(date);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=issues-" + date + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}