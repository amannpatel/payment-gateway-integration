package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.dto.request.SimulateDelayedWebhookRequest;
import com.example.paymentgatewayintegration.dto.request.SimulateDuplicateWebhookRequest;
import com.example.paymentgatewayintegration.dto.request.SimulateStatusDriftRequest;
import com.example.paymentgatewayintegration.dto.response.SimulationActionResponse;
import com.example.paymentgatewayintegration.entity.PaymentOrder;
import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.entity.RetryTask;
import com.example.paymentgatewayintegration.enums.PaymentEventSource;
import com.example.paymentgatewayintegration.enums.PaymentStatus;
import com.example.paymentgatewayintegration.gateway.PaymentGatewayClient;
import com.example.paymentgatewayintegration.repository.PaymentOrderRepository;
import com.example.paymentgatewayintegration.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SimulationService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGatewayRegistry paymentGatewayRegistry;
    private final WebhookService webhookService;
    private final RetryTaskService retryTaskService;
    private final PaymentLifecycleService paymentLifecycleService;

    public SimulationService(
            PaymentOrderRepository paymentOrderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentGatewayRegistry paymentGatewayRegistry,
            WebhookService webhookService,
            RetryTaskService retryTaskService,
            PaymentLifecycleService paymentLifecycleService
    ) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentGatewayRegistry = paymentGatewayRegistry;
        this.webhookService = webhookService;
        this.retryTaskService = retryTaskService;
        this.paymentLifecycleService = paymentLifecycleService;
    }

    public void scheduleWebhookDispatch(PaymentTransaction transaction, String eventType, long delaySeconds) {
        String gatewayEventId = "evt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        retryTaskService.enqueueWebhookDispatch(transaction, eventType, delaySeconds, gatewayEventId);
    }

    @Transactional
    public SimulationActionResponse simulateDuplicateWebhook(SimulateDuplicateWebhookRequest request) {
        PaymentTransaction transaction = resolveTransaction(request.merchantOrderId(), request.paymentId());
        PaymentOrder paymentOrder = transaction.getPaymentOrder();
        PaymentGatewayClient gatewayClient = paymentGatewayRegistry.resolve(paymentOrder.getGateway());
        String eventType = request.eventType() == null || request.eventType().isBlank()
                ? (paymentOrder.getStatus() == PaymentStatus.FAILED ? "payment.failed" : "payment.captured")
                : request.eventType();
        String gatewayEventId = request.gatewayEventId() == null || request.gatewayEventId().isBlank()
                ? "evt_dup_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14)
                : request.gatewayEventId();
        String payload = gatewayClient.buildWebhookEventPayload(eventType, gatewayEventId, paymentOrder, transaction);
        String signature = gatewayClient.signWebhookPayload(payload);

        webhookService.receiveRazorpayWebhook(payload, signature, gatewayEventId, UUID.randomUUID().toString(), false);
        webhookService.receiveRazorpayWebhook(payload, signature, gatewayEventId, UUID.randomUUID().toString(), false);

        return new SimulationActionResponse(
                "duplicate-webhook",
                "COMPLETED",
                "Sent two webhook deliveries with the same gateway event id",
                gatewayEventId,
                OffsetDateTime.now()
        );
    }

    @Transactional
    public SimulationActionResponse simulateDelayedWebhook(SimulateDelayedWebhookRequest request) {
        PaymentTransaction transaction = resolveTransaction(request.merchantOrderId(), request.paymentId());
        String gatewayEventId = "evt_delay_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        RetryTask task = retryTaskService.enqueueWebhookDispatch(transaction, request.eventType(), request.delaySeconds(), gatewayEventId);
        return new SimulationActionResponse(
                "delayed-webhook",
                "SCHEDULED",
                "Queued webhook delivery for delayed processing",
                gatewayEventId,
                task.getNextAttemptAt()
        );
    }

    @Transactional
    public SimulationActionResponse simulatePaymentSuccessDbMiss(SimulateStatusDriftRequest request) {
        PaymentTransaction transaction = resolveTransaction(request.merchantOrderId(), request.paymentId());
        transaction.setFailureCode("DB_NOT_UPDATED");
        transaction.setFailureReason(request.note() == null || request.note().isBlank()
                ? "Gateway succeeded but local state was forced to stale/failed for troubleshooting"
                : request.note());
        paymentLifecycleService.transition(transaction, PaymentStatus.FAILED, PaymentEventSource.SIMULATION, "manual-status-drift", transaction.getFailureReason());
        retryTaskService.enqueueMerchantViewReconciliation(transaction, "Manual status drift simulation");
        scheduleWebhookDispatch(transaction, "payment.captured", 8);
        return new SimulationActionResponse(
                "payment-success-db-miss",
                "SIMULATED",
                "Forced merchant-visible status drift and queued reconciliation",
                null,
                OffsetDateTime.now().plusSeconds(8)
        );
    }

    public void dispatchWebhookTask(RetryTask task, WebhookDispatchPayload payload) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(payload.transactionId())
                .orElseThrow(() -> new com.example.paymentgatewayintegration.exception.ResourceNotFoundException("Transaction not found for retry task " + task.getId()));
        PaymentOrder paymentOrder = transaction.getPaymentOrder();
        PaymentGatewayClient gatewayClient = paymentGatewayRegistry.resolve(paymentOrder.getGateway());
        String webhookPayload = gatewayClient.buildWebhookEventPayload(payload.eventType(), payload.gatewayEventId(), paymentOrder, transaction);
        String signature = gatewayClient.signWebhookPayload(webhookPayload);
        webhookService.receiveRazorpayWebhook(webhookPayload, signature, payload.gatewayEventId(), UUID.randomUUID().toString(), false);
    }

    private PaymentTransaction resolveTransaction(String merchantOrderId, String paymentId) {
        if (paymentId != null && !paymentId.isBlank()) {
            return paymentTransactionRepository.findByGatewayPaymentId(paymentId)
                    .orElseThrow(() -> new com.example.paymentgatewayintegration.exception.ResourceNotFoundException("Transaction not found for payment id " + paymentId));
        }
        if (merchantOrderId != null && !merchantOrderId.isBlank()) {
            PaymentOrder paymentOrder = paymentOrderRepository.findByMerchantOrderId(merchantOrderId)
                    .orElseThrow(() -> new com.example.paymentgatewayintegration.exception.ResourceNotFoundException("Order not found for merchant order id " + merchantOrderId));
            return paymentTransactionRepository.findTopByPaymentOrderIdOrderByAttemptNumberDesc(paymentOrder.getId())
                    .orElseThrow(() -> new com.example.paymentgatewayintegration.exception.ResourceNotFoundException("No transaction attempts found for order " + merchantOrderId));
        }
        throw new com.example.paymentgatewayintegration.exception.ResourceNotFoundException("Provide merchantOrderId or paymentId to simulate a webhook scenario");
    }
}