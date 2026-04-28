package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.dto.request.CreateIssueRequest;
import com.example.paymentgatewayintegration.dto.request.UpdateIssueStatusRequest;
import com.example.paymentgatewayintegration.dto.response.MerchantIssueResponse;
import com.example.paymentgatewayintegration.entity.MerchantIssue;
import com.example.paymentgatewayintegration.entity.PaymentOrder;
import com.example.paymentgatewayintegration.enums.IssueStatus;
import com.example.paymentgatewayintegration.exception.ResourceNotFoundException;
import com.example.paymentgatewayintegration.repository.MerchantIssueRepository;
import com.example.paymentgatewayintegration.repository.PaymentOrderRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
public class IssueService {

    private final MerchantIssueRepository merchantIssueRepository;
    private final PaymentOrderRepository paymentOrderRepository;

    public IssueService(MerchantIssueRepository merchantIssueRepository, PaymentOrderRepository paymentOrderRepository) {
        this.merchantIssueRepository = merchantIssueRepository;
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Transactional
    public MerchantIssueResponse createIssue(CreateIssueRequest request) {
        MerchantIssue issue = new MerchantIssue();
        issue.setMerchantId(request.merchantId());
        if (request.merchantOrderId() != null && !request.merchantOrderId().isBlank()) {
            PaymentOrder paymentOrder = paymentOrderRepository.findByMerchantOrderId(request.merchantOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + request.merchantOrderId()));
            issue.setPaymentOrder(paymentOrder);
        }
        issue.setPaymentId(request.paymentId());
        issue.setIssueType(request.issueType());
        issue.setStatus(IssueStatus.OPEN);
        issue.setSummary(request.summary());
        issue.setDescription(request.description());
        return toResponse(merchantIssueRepository.save(issue));
    }

    @Transactional
    public MerchantIssueResponse updateIssueStatus(Long issueId, UpdateIssueStatusRequest request) {
        MerchantIssue issue = merchantIssueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));
        issue.setStatus(request.status());
        issue.setAssignedTo(request.assignedTo());
        issue.setResolutionNotes(request.resolutionNotes());
        return toResponse(merchantIssueRepository.save(issue));
    }

    @Transactional(readOnly = true)
    public List<MerchantIssueResponse> listIssues(IssueStatus status) {
        List<MerchantIssue> issues = status == null
                ? merchantIssueRepository.findAll().stream().sorted(Comparator.comparing(MerchantIssue::getCreatedAt).reversed()).toList()
                : merchantIssueRepository.findAllByStatusOrderByCreatedAtDesc(status);
        return issues.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public byte[] exportDailyIssues(LocalDate date) {
        OffsetDateTime from = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);
        List<MerchantIssue> issues = merchantIssueRepository.findAllByCreatedAtBetweenOrderByCreatedAtDesc(from, to);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Daily Issues");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Issue ID");
            header.createCell(1).setCellValue("Merchant ID");
            header.createCell(2).setCellValue("Merchant Order ID");
            header.createCell(3).setCellValue("Payment ID");
            header.createCell(4).setCellValue("Type");
            header.createCell(5).setCellValue("Status");
            header.createCell(6).setCellValue("Summary");
            header.createCell(7).setCellValue("Assigned To");
            header.createCell(8).setCellValue("Created At");

            int rowIndex = 1;
            for (MerchantIssue issue : issues) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(issue.getId());
                row.createCell(1).setCellValue(issue.getMerchantId());
                row.createCell(2).setCellValue(issue.getPaymentOrder() == null ? "" : issue.getPaymentOrder().getMerchantOrderId());
                row.createCell(3).setCellValue(issue.getPaymentId() == null ? "" : issue.getPaymentId());
                row.createCell(4).setCellValue(issue.getIssueType().name());
                row.createCell(5).setCellValue(issue.getStatus().name());
                row.createCell(6).setCellValue(issue.getSummary());
                row.createCell(7).setCellValue(issue.getAssignedTo() == null ? "" : issue.getAssignedTo());
                row.createCell(8).setCellValue(issue.getCreatedAt().toString());
            }

            for (int column = 0; column < 9; column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to export issue workbook", exception);
        }
    }

    private MerchantIssueResponse toResponse(MerchantIssue issue) {
        return new MerchantIssueResponse(
                issue.getId(),
                issue.getMerchantId(),
                issue.getPaymentOrder() == null ? null : issue.getPaymentOrder().getMerchantOrderId(),
                issue.getPaymentId(),
                issue.getIssueType(),
                issue.getStatus(),
                issue.getSummary(),
                issue.getDescription(),
                issue.getAssignedTo(),
                issue.getResolutionNotes(),
                issue.getCreatedAt(),
                issue.getUpdatedAt()
        );
    }
}