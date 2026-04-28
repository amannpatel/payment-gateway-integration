package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.common.CorrelationIdFilter;
import com.example.paymentgatewayintegration.dto.request.CreatePaymentOrderRequest;
import com.example.paymentgatewayintegration.dto.request.InitiatePaymentRequest;
import com.example.paymentgatewayintegration.dto.response.PaymentAttemptResponse;
import com.example.paymentgatewayintegration.dto.response.PaymentHistoryEntryResponse;
import com.example.paymentgatewayintegration.dto.response.PaymentInitiationResponse;
import com.example.paymentgatewayintegration.dto.response.PaymentOrderResponse;
import com.example.paymentgatewayintegration.dto.response.PaymentStatusResponse;
import com.example.paymentgatewayintegration.entity.PaymentOrder;
import com.example.paymentgatewayintegration.entity.PaymentStatusHistory;
import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.enums.PaymentEventSource;
import com.example.paymentgatewayintegration.enums.PaymentStatus;
import com.example.paymentgatewayintegration.exception.DuplicateRequestException;
import com.example.paymentgatewayintegration.exception.GatewayIntegrationException;
import com.example.paymentgatewayintegration.exception.ResourceNotFoundException;
import com.example.paymentgatewayintegration.gateway.GatewayOrderRequest;
import com.example.paymentgatewayintegration.gateway.GatewayOrderResponse;
import com.example.paymentgatewayintegration.gateway.GatewayPaymentRequest;
import com.example.paymentgatewayintegration.gateway.GatewayPaymentResponse;
import com.example.paymentgatewayintegration.gateway.PaymentGatewayClient;
import com.example.paymentgatewayintegration.repository.PaymentOrderRepository;
import com.example.paymentgatewayintegration.repository.PaymentStatusHistoryRepository;
import com.example.paymentgatewayintegration.repository.PaymentTransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final PaymentGatewayRegistry paymentGatewayRegistry;
    private final PaymentLifecycleService paymentLifecycleService;
    private final RetryTaskService retryTaskService;
    private final SimulationService simulationService;
    private final ObjectMapper objectMapper;

    public PaymentService(
            PaymentOrderRepository paymentOrderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository,
            PaymentGatewayRegistry paymentGatewayRegistry,
            PaymentLifecycleService paymentLifecycleService,
            RetryTaskService retryTaskService,
            SimulationService simulationService,
            ObjectMapper objectMapper
    ) {
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
        this.paymentGatewayRegistry = paymentGatewayRegistry;
        this.paymentLifecycleService = paymentLifecycleService;
        this.retryTaskService = retryTaskService;
        this.simulationService = simulationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentOrderResponse createOrder(CreatePaymentOrderRequest request) {
        paymentOrderRepository.findByMerchantIdAndMerchantOrderId(request.merchantId(), request.merchantOrderId())
                .ifPresent(existing -> {
                    throw new DuplicateRequestException("Merchant order already exists: " + request.merchantOrderId());
                });

        PaymentGatewayClient gatewayClient = paymentGatewayRegistry.resolve(request.gateway());
        GatewayOrderResponse gatewayResponse = gatewayClient.createOrder(new GatewayOrderRequest(
                request.merchantOrderId(),
                request.amount(),
                request.currency(),
                request.description(),
                request.metadata() == null ? Map.of() : request.metadata()
        ));

        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setMerchantId(request.merchantId());
        paymentOrder.setMerchantOrderId(request.merchantOrderId());
        paymentOrder.setGateway(request.gateway());
        paymentOrder.setAmount(request.amount());
        paymentOrder.setCurrency(request.currency());
        paymentOrder.setDescription(request.description());
        paymentOrder.setCustomerEmail(request.customerEmail());
        paymentOrder.setCustomerPhone(request.customerPhone());
        paymentOrder.setStatus(PaymentStatus.CREATED);
        paymentOrder.setGatewayOrderId(gatewayResponse.gatewayOrderId());
        paymentOrder.setCorrelationId(MDC.get(CorrelationIdFilter.CORRELATION_ID));
        paymentOrder.setMetadataJson(writeJson(request.metadata()));

        PaymentOrder savedOrder = paymentOrderRepository.save(paymentOrder);
        log.info("payment_order_created merchantId={} merchantOrderId={} gateway={} gatewayOrderId={}", savedOrder.getMerchantId(), savedOrder.getMerchantOrderId(), savedOrder.getGateway(), savedOrder.getGatewayOrderId());
        return toOrderResponse(savedOrder);
    }

    @Transactional
    public PaymentInitiationResponse initiatePayment(String merchantOrderId, InitiatePaymentRequest request) {
        PaymentOrder paymentOrder = paymentOrderRepository.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + merchantOrderId));
        PaymentGatewayClient gatewayClient = paymentGatewayRegistry.resolve(paymentOrder.getGateway());

        int nextAttemptNumber = paymentTransactionRepository.findTopByPaymentOrderIdOrderByAttemptNumberDesc(paymentOrder.getId())
                .map(existing -> existing.getAttemptNumber() + 1)
                .orElse(1);

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setPaymentOrder(paymentOrder);
        transaction.setAttemptNumber(nextAttemptNumber);
        transaction.setGateway(paymentOrder.getGateway());
        transaction.setGatewayOrderId(paymentOrder.getGatewayOrderId());
        transaction.setStatus(PaymentStatus.CREATED);
        transaction.setAmount(paymentOrder.getAmount());
        transaction.setCurrency(paymentOrder.getCurrency());
        transaction.setPaymentMethod(request.paymentMethod());
        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        GatewayPaymentResponse gatewayPaymentResponse = gatewayClient.initiatePayment(new GatewayPaymentRequest(
                paymentOrder.getGatewayOrderId(),
                paymentOrder.getAmount(),
                paymentOrder.getCurrency(),
                request.paymentMethod(),
                request.scenario(),
                request.webhookDelaySeconds() == null ? 0 : request.webhookDelaySeconds()
        ));

        savedTransaction.setGatewayPaymentId(gatewayPaymentResponse.gatewayPaymentId());
        savedTransaction.setAuthorizationId(gatewayPaymentResponse.authorizationId());
        savedTransaction.setGatewayReference(gatewayPaymentResponse.gatewayReference());
        savedTransaction.setGatewayRawResponse(gatewayPaymentResponse.rawResponse());
        savedTransaction.setFailureCode(gatewayPaymentResponse.failureCode());
        savedTransaction.setFailureReason(gatewayPaymentResponse.failureReason());
        paymentOrder.setLatestPaymentId(gatewayPaymentResponse.gatewayPaymentId());
        paymentTransactionRepository.save(savedTransaction);
        paymentOrderRepository.save(paymentOrder);

        boolean reconciliationQueued = false;
        String message;
        if (gatewayPaymentResponse.timedOut() || gatewayPaymentResponse.merchantViewShouldFail()) {
            paymentLifecycleService.transition(savedTransaction, PaymentStatus.FAILED, PaymentEventSource.API, "merchant:initiate", gatewayPaymentResponse.failureReason());
            retryTaskService.enqueuePaymentReconciliation(savedTransaction, gatewayPaymentResponse.failureReason());
            reconciliationQueued = true;
            message = gatewayPaymentResponse.timedOut()
                    ? "Gateway timed out. Local status is failed until reconciliation or webhook repair completes."
                    : "Payment succeeded upstream but local state was intentionally left stale for troubleshooting.";
        } else {
            paymentLifecycleService.transition(savedTransaction, gatewayPaymentResponse.status(), PaymentEventSource.API, "merchant:initiate", "Payment initiated via merchant API");
            message = gatewayPaymentResponse.status() == PaymentStatus.AUTHORIZED
                    ? "Payment authorized. Capture webhook is expected shortly."
                    : gatewayPaymentResponse.status() == PaymentStatus.FAILED
                    ? "Payment failed at gateway."
                    : "Payment updated successfully.";
        }

        if (gatewayPaymentResponse.shouldScheduleWebhook() && gatewayPaymentResponse.webhookEventType() != null) {
            simulationService.scheduleWebhookDispatch(savedTransaction, gatewayPaymentResponse.webhookEventType(), gatewayPaymentResponse.webhookDelaySeconds());
        }

        PaymentTransaction latestTransaction = paymentTransactionRepository.findById(savedTransaction.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction disappeared after initiation"));
        return new PaymentInitiationResponse(
                paymentOrder.getMerchantOrderId(),
                paymentOrder.getGatewayOrderId(),
                latestTransaction.getGatewayPaymentId(),
                latestTransaction.getStatus(),
                gatewayPaymentResponse.gatewayFinalStatus(),
                reconciliationQueued,
                gatewayPaymentResponse.shouldScheduleWebhook(),
                message,
                latestTransaction.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public PaymentStatusResponse getPaymentStatus(String merchantOrderId) {
        PaymentOrder paymentOrder = paymentOrderRepository.findByMerchantOrderId(merchantOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found: " + merchantOrderId));
        List<PaymentTransaction> attempts = paymentTransactionRepository.findAllByPaymentOrderIdOrderByAttemptNumberAsc(paymentOrder.getId());
        PaymentTransaction latestTransaction = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);

        return new PaymentStatusResponse(
                paymentOrder.getMerchantId(),
                paymentOrder.getMerchantOrderId(),
                paymentOrder.getGateway(),
                paymentOrder.getGatewayOrderId(),
                latestTransaction == null ? null : latestTransaction.getGatewayPaymentId(),
                latestTransaction == null ? paymentOrder.getStatus() : latestTransaction.getStatus(),
                paymentOrder.getAmount(),
                paymentOrder.getCurrency(),
                latestTransaction == null ? paymentOrder.getFailureReason() : latestTransaction.getFailureReason(),
                paymentOrder.getUpdatedAt(),
                attempts.stream().map(this::toAttemptResponse).toList(),
                buildHistory(attempts)
        );
    }

    private List<PaymentHistoryEntryResponse> buildHistory(List<PaymentTransaction> attempts) {
        if (attempts.isEmpty()) {
            return Collections.emptyList();
        }
        List<PaymentHistoryEntryResponse> historyEntries = new ArrayList<>();
        for (PaymentTransaction attempt : attempts) {
            List<PaymentStatusHistory> history = paymentStatusHistoryRepository.findAllByPaymentTransactionIdOrderByCreatedAtAsc(attempt.getId());
            history.stream().map(entry -> toHistoryResponse(attempt.getAttemptNumber(), entry)).forEach(historyEntries::add);
        }
        return historyEntries;
    }

    private PaymentOrderResponse toOrderResponse(PaymentOrder paymentOrder) {
        return new PaymentOrderResponse(
                paymentOrder.getMerchantId(),
                paymentOrder.getMerchantOrderId(),
                paymentOrder.getGateway(),
                paymentOrder.getAmount(),
                paymentOrder.getCurrency(),
                paymentOrder.getStatus(),
                paymentOrder.getGatewayOrderId(),
                paymentOrder.getLatestPaymentId(),
                paymentOrder.getCreatedAt(),
                paymentOrder.getUpdatedAt()
        );
    }

    private PaymentAttemptResponse toAttemptResponse(PaymentTransaction transaction) {
        return new PaymentAttemptResponse(
                transaction.getAttemptNumber(),
                transaction.getGatewayPaymentId(),
                transaction.getStatus(),
                transaction.getFailureCode(),
                transaction.getFailureReason(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    private PaymentHistoryEntryResponse toHistoryResponse(Integer attemptNumber, PaymentStatusHistory history) {
        return new PaymentHistoryEntryResponse(
                attemptNumber,
                history.getFromStatus(),
                history.getToStatus(),
                history.getSource(),
                history.getSourceReference(),
                history.getNotes(),
                history.getCreatedAt()
        );
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new GatewayIntegrationException("Unable to serialize order metadata", exception);
        }
    }
}