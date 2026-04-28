package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.dto.response.RetryTaskResponse;
import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.entity.RetryTask;
import com.example.paymentgatewayintegration.enums.RetryTaskStatus;
import com.example.paymentgatewayintegration.enums.RetryTaskType;
import com.example.paymentgatewayintegration.exception.GatewayIntegrationException;
import com.example.paymentgatewayintegration.repository.RetryTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class RetryTaskService {

    private final RetryTaskRepository retryTaskRepository;
    private final ObjectMapper objectMapper;

    public RetryTaskService(RetryTaskRepository retryTaskRepository, ObjectMapper objectMapper) {
        this.retryTaskRepository = retryTaskRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RetryTask enqueuePaymentReconciliation(PaymentTransaction transaction, String reason) {
        PaymentReconciliationPayload payload = new PaymentReconciliationPayload(
                transaction.getGatewayPaymentId(),
                transaction.getGatewayOrderId(),
                reason
        );
        return saveTask(RetryTaskType.PAYMENT_STATUS_RECONCILIATION, "PAYMENT_TRANSACTION", String.valueOf(transaction.getId()), toJson(payload), 15, 5);
    }

    @Transactional
    public RetryTask enqueueMerchantViewReconciliation(PaymentTransaction transaction, String reason) {
        PaymentReconciliationPayload payload = new PaymentReconciliationPayload(
                transaction.getGatewayPaymentId(),
                transaction.getGatewayOrderId(),
                reason
        );
        return saveTask(RetryTaskType.MERCHANT_VIEW_RECONCILIATION, "PAYMENT_TRANSACTION", String.valueOf(transaction.getId()), toJson(payload), 10, 5);
    }

    @Transactional
    public RetryTask enqueueWebhookDispatch(PaymentTransaction transaction, String eventType, long delaySeconds, String gatewayEventId) {
        WebhookDispatchPayload payload = new WebhookDispatchPayload(transaction.getId(), eventType, gatewayEventId);
        return saveTask(RetryTaskType.WEBHOOK_DISPATCH, "PAYMENT_TRANSACTION", String.valueOf(transaction.getId()), toJson(payload), delaySeconds, 3);
    }

    public List<RetryTaskResponse> listTasks() {
        return retryTaskRepository.findAll().stream()
                .sorted(Comparator.comparing(RetryTask::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public RetryTaskRepository repository() {
        return retryTaskRepository;
    }

    private RetryTask saveTask(RetryTaskType taskType, String entityType, String entityId, String payload, long delaySeconds, int maxAttempts) {
        RetryTask task = new RetryTask();
        task.setTaskType(taskType);
        task.setStatus(RetryTaskStatus.PENDING);
        task.setEntityType(entityType);
        task.setEntityId(entityId);
        task.setPayload(payload);
        task.setAttemptCount(0);
        task.setMaxAttempts(maxAttempts);
        task.setNextAttemptAt(OffsetDateTime.now().plusSeconds(Math.max(1, delaySeconds)));
        return retryTaskRepository.save(task);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new GatewayIntegrationException("Unable to serialize retry payload", exception);
        }
    }

    private RetryTaskResponse toResponse(RetryTask task) {
        return new RetryTaskResponse(
                task.getId(),
                task.getTaskType(),
                task.getStatus(),
                task.getEntityType(),
                task.getEntityId(),
                task.getAttemptCount(),
                task.getMaxAttempts(),
                task.getNextAttemptAt(),
                task.getLastError(),
                task.getUpdatedAt()
        );
    }
}