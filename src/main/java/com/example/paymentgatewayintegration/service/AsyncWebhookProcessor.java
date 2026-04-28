package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.common.CorrelationIdFilter;
import com.example.paymentgatewayintegration.entity.PaymentOrder;
import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.entity.WebhookEvent;
import com.example.paymentgatewayintegration.enums.PaymentEventSource;
import com.example.paymentgatewayintegration.enums.PaymentStatus;
import com.example.paymentgatewayintegration.enums.WebhookProcessingStatus;
import com.example.paymentgatewayintegration.exception.ResourceNotFoundException;
import com.example.paymentgatewayintegration.repository.PaymentOrderRepository;
import com.example.paymentgatewayintegration.repository.PaymentTransactionRepository;
import com.example.paymentgatewayintegration.repository.WebhookEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AsyncWebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncWebhookProcessor.class);

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentLifecycleService paymentLifecycleService;
    private final RetryTaskService retryTaskService;

    public AsyncWebhookProcessor(
            WebhookEventRepository webhookEventRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentOrderRepository paymentOrderRepository,
            IdempotencyService idempotencyService,
            PaymentLifecycleService paymentLifecycleService,
            RetryTaskService retryTaskService
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.idempotencyService = idempotencyService;
        this.paymentLifecycleService = paymentLifecycleService;
        this.retryTaskService = retryTaskService;
    }

    @Async("webhookTaskExecutor")
    public void processWebhookAsync(Long webhookEventId) {
        processStoredWebhook(webhookEventId);
    }

    @Transactional
    public void processStoredWebhook(Long webhookEventId) {
        WebhookEvent webhookEvent = webhookEventRepository.findById(webhookEventId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook event not found: " + webhookEventId));

        MDC.put(CorrelationIdFilter.CORRELATION_ID, webhookEvent.getCorrelationId());
        webhookEvent.setProcessingStatus(WebhookProcessingStatus.PROCESSING);
        webhookEventRepository.save(webhookEvent);

        String eventKey = "webhook:%s:%s".formatted(webhookEvent.getGateway().name().toLowerCase(), webhookEvent.getGatewayEventId());
        try {
            boolean firstProcess = idempotencyService.recordProcessedEvent(
                    eventKey,
                    webhookEvent.getEventType(),
                    "WEBHOOK",
                    webhookEvent.getGatewayEventId(),
                    webhookEvent.getPayloadHash(),
                    webhookEvent.getCorrelationId()
            );
            if (!firstProcess) {
                webhookEvent.setDuplicateDelivery(true);
                webhookEvent.setProcessingStatus(WebhookProcessingStatus.DUPLICATE);
                webhookEvent.setProcessedAt(OffsetDateTime.now());
                webhookEventRepository.save(webhookEvent);
                return;
            }

            PaymentTransaction transaction = resolveTransaction(webhookEvent);
            transaction.setLastProcessedEventKey(eventKey);
            if ("payment.captured".equals(webhookEvent.getEventType())) {
                paymentLifecycleService.transition(transaction, PaymentStatus.CAPTURED, PaymentEventSource.WEBHOOK, webhookEvent.getGatewayEventId(), "Captured via gateway webhook");
            } else if ("payment.failed".equals(webhookEvent.getEventType())) {
                if (transaction.getFailureReason() == null) {
                    transaction.setFailureReason("Failed via gateway webhook");
                }
                paymentLifecycleService.transition(transaction, PaymentStatus.FAILED, PaymentEventSource.WEBHOOK, webhookEvent.getGatewayEventId(), "Failure received via gateway webhook");
            } else if ("payment.authorized".equals(webhookEvent.getEventType())) {
                paymentLifecycleService.transition(transaction, PaymentStatus.AUTHORIZED, PaymentEventSource.WEBHOOK, webhookEvent.getGatewayEventId(), "Authorized via gateway webhook");
            }

            webhookEvent.setProcessingStatus(WebhookProcessingStatus.PROCESSED);
            webhookEvent.setProcessedAt(OffsetDateTime.now());
            webhookEventRepository.save(webhookEvent);
            log.info("webhook_processed eventId={} paymentId={} eventType={}", webhookEvent.getGatewayEventId(), webhookEvent.getPaymentId(), webhookEvent.getEventType());
        } catch (Exception exception) {
            webhookEvent.setProcessingStatus(WebhookProcessingStatus.FAILED);
            webhookEvent.setProcessingError(exception.getMessage());
            webhookEventRepository.save(webhookEvent);
            log.error("webhook_processing_failed eventId={} reason={}", webhookEvent.getGatewayEventId(), exception.getMessage(), exception);
            resolveTransactionSafely(webhookEvent).ifPresent(transaction -> retryTaskService.enqueuePaymentReconciliation(transaction, "Webhook processing failure: " + exception.getMessage()));
        } finally {
            MDC.remove(CorrelationIdFilter.CORRELATION_ID);
        }
    }

    private java.util.Optional<PaymentTransaction> resolveTransactionSafely(WebhookEvent webhookEvent) {
        try {
            return java.util.Optional.of(resolveTransaction(webhookEvent));
        } catch (Exception exception) {
            return java.util.Optional.empty();
        }
    }

    private PaymentTransaction resolveTransaction(WebhookEvent webhookEvent) {
        if (webhookEvent.getPaymentId() != null) {
            return paymentTransactionRepository.findByGatewayPaymentId(webhookEvent.getPaymentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for payment " + webhookEvent.getPaymentId()));
        }
        if (webhookEvent.getOrderId() != null) {
            PaymentOrder paymentOrder = paymentOrderRepository.findByGatewayOrderId(webhookEvent.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found for gateway order " + webhookEvent.getOrderId()));
            return paymentTransactionRepository.findTopByPaymentOrderIdOrderByAttemptNumberDesc(paymentOrder.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for order " + paymentOrder.getMerchantOrderId()));
        }
        throw new ResourceNotFoundException("Webhook did not contain order or payment identifiers");
    }
}