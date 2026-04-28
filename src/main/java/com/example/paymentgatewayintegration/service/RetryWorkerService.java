package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.entity.RetryTask;
import com.example.paymentgatewayintegration.enums.PaymentEventSource;
import com.example.paymentgatewayintegration.enums.RetryTaskStatus;
import com.example.paymentgatewayintegration.enums.RetryTaskType;
import com.example.paymentgatewayintegration.gateway.GatewayStatusResponse;
import com.example.paymentgatewayintegration.gateway.PaymentGatewayClient;
import com.example.paymentgatewayintegration.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RetryWorkerService {

    private static final Logger log = LoggerFactory.getLogger(RetryWorkerService.class);

    private final RetryTaskService retryTaskService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentGatewayRegistry paymentGatewayRegistry;
    private final PaymentLifecycleService paymentLifecycleService;
    private final SimulationService simulationService;
    private final ObjectMapper objectMapper;

    public RetryWorkerService(
            RetryTaskService retryTaskService,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentGatewayRegistry paymentGatewayRegistry,
            PaymentLifecycleService paymentLifecycleService,
            SimulationService simulationService,
            ObjectMapper objectMapper
    ) {
        this.retryTaskService = retryTaskService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentGatewayRegistry = paymentGatewayRegistry;
        this.paymentLifecycleService = paymentLifecycleService;
        this.simulationService = simulationService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.retry.poll-interval-ms:15000}")
    public void processDueRetries() {
        List<RetryTask> tasks = retryTaskService.repository().findTop50ByStatusInAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(
                List.of(RetryTaskStatus.PENDING, RetryTaskStatus.FAILED),
                OffsetDateTime.now().plusSeconds(1)
        );
        tasks.forEach(this::processTask);
    }

    @Transactional
    public void processTask(RetryTask task) {
        try {
            task.setStatus(RetryTaskStatus.PROCESSING);
            task.setAttemptCount(task.getAttemptCount() + 1);
            task.setLastAttemptAt(OffsetDateTime.now());
            retryTaskService.repository().save(task);

            if (task.getTaskType() == RetryTaskType.WEBHOOK_DISPATCH) {
                simulationService.dispatchWebhookTask(task, readWebhookPayload(task.getPayload()));
            } else {
                reconcilePayment(task, readReconciliationPayload(task.getPayload()));
            }

            task.setStatus(RetryTaskStatus.COMPLETED);
            task.setLastError(null);
            task.setNextAttemptAt(OffsetDateTime.now());
            retryTaskService.repository().save(task);
        } catch (Exception exception) {
            log.error("retry_task_failed taskId={} type={} reason={}", task.getId(), task.getTaskType(), exception.getMessage(), exception);
            task.setLastError(exception.getMessage());
            if (task.getAttemptCount() >= task.getMaxAttempts()) {
                task.setStatus(RetryTaskStatus.DEAD_LETTER);
            } else {
                task.setStatus(RetryTaskStatus.FAILED);
                task.setNextAttemptAt(OffsetDateTime.now().plusSeconds((long) Math.pow(2, task.getAttemptCount()) * 10));
            }
            retryTaskService.repository().save(task);
        }
    }

    private void reconcilePayment(RetryTask task, PaymentReconciliationPayload payload) {
        PaymentTransaction transaction = paymentTransactionRepository.findById(Long.valueOf(task.getEntityId()))
                .orElseThrow(() -> new com.example.paymentgatewayintegration.exception.ResourceNotFoundException("Transaction not found for retry task " + task.getId()));
        PaymentGatewayClient gatewayClient = paymentGatewayRegistry.resolve(transaction.getPaymentOrder().getGateway());
        GatewayStatusResponse gatewayStatus = gatewayClient.fetchPaymentStatus(payload.gatewayPaymentId(), payload.gatewayOrderId());
        transaction.setFailureCode(gatewayStatus.failureCode());
        transaction.setFailureReason(gatewayStatus.failureReason());
        paymentLifecycleService.transition(
                transaction,
                gatewayStatus.status(),
                PaymentEventSource.RETRY,
                "retry-task:" + task.getId(),
                payload.reason()
        );
    }

    private PaymentReconciliationPayload readReconciliationPayload(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentReconciliationPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse retry payload", exception);
        }
    }

    private WebhookDispatchPayload readWebhookPayload(String payload) {
        try {
            return objectMapper.readValue(payload, WebhookDispatchPayload.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to parse webhook dispatch payload", exception);
        }
    }
}