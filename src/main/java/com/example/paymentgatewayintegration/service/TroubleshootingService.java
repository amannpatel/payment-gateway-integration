package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.dto.response.PaymentAttemptResponse;
import com.example.paymentgatewayintegration.dto.response.PaymentHistoryEntryResponse;
import com.example.paymentgatewayintegration.dto.response.TransactionDiagnosticsResponse;
import com.example.paymentgatewayintegration.dto.response.WebhookEventResponse;
import com.example.paymentgatewayintegration.entity.PaymentOrder;
import com.example.paymentgatewayintegration.entity.PaymentStatusHistory;
import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.exception.ResourceNotFoundException;
import com.example.paymentgatewayintegration.gateway.GatewayStatusResponse;
import com.example.paymentgatewayintegration.repository.PaymentOrderRepository;
import com.example.paymentgatewayintegration.repository.PaymentStatusHistoryRepository;
import com.example.paymentgatewayintegration.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class TroubleshootingService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final PaymentGatewayRegistry paymentGatewayRegistry;
    private final WebhookService webhookService;
    private final RetryTaskService retryTaskService;

    public TroubleshootingService(
            PaymentOrderRepository paymentOrderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository,
            PaymentGatewayRegistry paymentGatewayRegistry,
            WebhookService webhookService,
            RetryTaskService retryTaskService
    ) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
        this.paymentGatewayRegistry = paymentGatewayRegistry;
        this.webhookService = webhookService;
        this.retryTaskService = retryTaskService;
    }

    @Transactional(readOnly = true)
    public TransactionDiagnosticsResponse searchTransactions(String merchantOrderId, String paymentId) {
        PaymentTransaction latestTransaction;
        PaymentOrder paymentOrder;

        if (paymentId != null && !paymentId.isBlank()) {
            latestTransaction = paymentTransactionRepository.findByGatewayPaymentId(paymentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found for payment id " + paymentId));
            paymentOrder = latestTransaction.getPaymentOrder();
        } else if (merchantOrderId != null && !merchantOrderId.isBlank()) {
            paymentOrder = paymentOrderRepository.findByMerchantOrderId(merchantOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment order not found for merchant order id " + merchantOrderId));
            latestTransaction = paymentTransactionRepository.findTopByPaymentOrderIdOrderByAttemptNumberDesc(paymentOrder.getId())
                    .orElse(null);
        } else {
            throw new ResourceNotFoundException("Provide merchantOrderId or paymentId to search transactions");
        }

        List<PaymentTransaction> attempts = paymentTransactionRepository.findAllByPaymentOrderIdOrderByAttemptNumberAsc(paymentOrder.getId());
        List<PaymentHistoryEntryResponse> history = new ArrayList<>();
        for (PaymentTransaction attempt : attempts) {
            List<PaymentStatusHistory> entries = paymentStatusHistoryRepository.findAllByPaymentTransactionIdOrderByCreatedAtAsc(attempt.getId());
            entries.stream().map(entry -> new PaymentHistoryEntryResponse(
                    attempt.getAttemptNumber(),
                    entry.getFromStatus(),
                    entry.getToStatus(),
                    entry.getSource(),
                    entry.getSourceReference(),
                    entry.getNotes(),
                    entry.getCreatedAt()
            )).forEach(history::add);
        }

        GatewayStatusResponse gatewayObserved = latestTransaction != null && latestTransaction.getGatewayPaymentId() != null
                ? paymentGatewayRegistry.resolve(paymentOrder.getGateway()).fetchPaymentStatus(latestTransaction.getGatewayPaymentId(), paymentOrder.getGatewayOrderId())
                : null;

        String webhookOrderId = paymentOrder.getGatewayOrderId();
        String webhookPaymentId = latestTransaction == null ? null : latestTransaction.getGatewayPaymentId();
        List<WebhookEventResponse> webhookEvents = webhookPaymentId != null
                ? webhookService.findWebhookEvents(null, webhookPaymentId)
                : webhookService.findWebhookEvents(webhookOrderId, null);

        return new TransactionDiagnosticsResponse(
                paymentOrder.getMerchantId(),
                paymentOrder.getMerchantOrderId(),
                paymentOrder.getGateway(),
                paymentOrder.getGatewayOrderId(),
                paymentOrder.getStatus(),
                gatewayObserved == null ? paymentOrder.getStatus() : gatewayObserved.status(),
                latestTransaction == null ? null : latestTransaction.getGatewayPaymentId(),
                paymentOrder.getAmount(),
                paymentOrder.getCurrency(),
                paymentOrder.getCorrelationId(),
                latestTransaction == null ? paymentOrder.getFailureReason() : latestTransaction.getFailureReason(),
                paymentOrder.getUpdatedAt(),
                attempts.stream().map(attempt -> new PaymentAttemptResponse(
                        attempt.getAttemptNumber(),
                        attempt.getGatewayPaymentId(),
                        attempt.getStatus(),
                        attempt.getFailureCode(),
                        attempt.getFailureReason(),
                        attempt.getCreatedAt(),
                        attempt.getUpdatedAt()
                )).toList(),
                history,
                webhookEvents
        );
    }

    public List<WebhookEventResponse> getWebhookLogs(String orderId, String paymentId) {
        return webhookService.findWebhookEvents(orderId, paymentId);
    }

    public java.util.List<com.example.paymentgatewayintegration.dto.response.RetryTaskResponse> listRetryTasks() {
        return retryTaskService.listTasks();
    }
}