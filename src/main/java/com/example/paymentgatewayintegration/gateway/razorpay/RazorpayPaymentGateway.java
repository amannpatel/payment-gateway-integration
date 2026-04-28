package com.example.paymentgatewayintegration.gateway.razorpay;

import com.example.paymentgatewayintegration.common.SecurityUtils;
import com.example.paymentgatewayintegration.entity.PaymentOrder;
import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.enums.GatewayType;
import com.example.paymentgatewayintegration.enums.PaymentStatus;
import com.example.paymentgatewayintegration.enums.SimulationScenario;
import com.example.paymentgatewayintegration.exception.GatewayIntegrationException;
import com.example.paymentgatewayintegration.gateway.GatewayOrderRequest;
import com.example.paymentgatewayintegration.gateway.GatewayOrderResponse;
import com.example.paymentgatewayintegration.gateway.GatewayPaymentRequest;
import com.example.paymentgatewayintegration.gateway.GatewayPaymentResponse;
import com.example.paymentgatewayintegration.gateway.GatewayStatusResponse;
import com.example.paymentgatewayintegration.gateway.PaymentGatewayClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RazorpayPaymentGateway implements PaymentGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(RazorpayPaymentGateway.class);

    private final RazorpayProperties properties;
    private final ObjectMapper objectMapper;

    public RazorpayPaymentGateway(RazorpayProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayType getGatewayType() {
        return GatewayType.RAZORPAY;
    }

    @Override
    public GatewayOrderResponse createOrder(GatewayOrderRequest request) {
        String gatewayOrderId = "order_rzp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", gatewayOrderId);
        response.put("entity", "order");
        response.put("amount", toMinorUnits(request.amount()));
        response.put("currency", request.currency());
        response.put("receipt", request.merchantOrderId());
        response.put("status", "created");
        log.info("gateway_order_created gateway={} merchantOrderId={} gatewayOrderId={} mockMode={}", getGatewayType(), request.merchantOrderId(), gatewayOrderId, properties.mockEnabled());
        return new GatewayOrderResponse(gatewayOrderId, toJson(response));
    }

    @Override
    public GatewayPaymentResponse initiatePayment(GatewayPaymentRequest request) {
        SimulationScenario scenario = request.scenario() == null ? SimulationScenario.SUCCESS : request.scenario();
        String gatewayPaymentId = buildPaymentId(scenario);
        String authorizationId = "auth_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String gatewayReference = "rzp_ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        long webhookDelay = request.webhookDelaySeconds() > 0 ? request.webhookDelaySeconds() : defaultDelaySeconds(scenario);

        GatewayPaymentResponse response = switch (scenario) {
            case SUCCESS -> new GatewayPaymentResponse(
                    gatewayPaymentId,
                    PaymentStatus.AUTHORIZED,
                    PaymentStatus.CAPTURED,
                    authorizationId,
                    gatewayReference,
                    toJson(paymentResponseBody(gatewayPaymentId, request.gatewayOrderId(), "authorized", request.amount(), request.currency(), gatewayReference)),
                    false,
                    false,
                    true,
                    "payment.captured",
                    webhookDelay,
                    null,
                    null
            );
            case AUTHORIZE_ONLY -> new GatewayPaymentResponse(
                    gatewayPaymentId,
                    PaymentStatus.AUTHORIZED,
                    PaymentStatus.AUTHORIZED,
                    authorizationId,
                    gatewayReference,
                    toJson(paymentResponseBody(gatewayPaymentId, request.gatewayOrderId(), "authorized", request.amount(), request.currency(), gatewayReference)),
                    false,
                    false,
                    false,
                    null,
                    0,
                    null,
                    null
            );
            case FAIL -> new GatewayPaymentResponse(
                    gatewayPaymentId,
                    PaymentStatus.FAILED,
                    PaymentStatus.FAILED,
                    null,
                    gatewayReference,
                    toJson(paymentResponseBody(gatewayPaymentId, request.gatewayOrderId(), "failed", request.amount(), request.currency(), gatewayReference)),
                    false,
                    false,
                    true,
                    "payment.failed",
                    webhookDelay,
                    "DECLINED_BY_BANK",
                    "Issuer declined the transaction"
            );
            case GATEWAY_TIMEOUT -> new GatewayPaymentResponse(
                    gatewayPaymentId,
                    PaymentStatus.FAILED,
                    PaymentStatus.CAPTURED,
                    authorizationId,
                    gatewayReference,
                    toJson(paymentResponseBody(gatewayPaymentId, request.gatewayOrderId(), "captured", request.amount(), request.currency(), gatewayReference)),
                    true,
                    false,
                    true,
                    "payment.captured",
                    webhookDelay,
                    "GATEWAY_TIMEOUT",
                    "Gateway timed out before the merchant system received the final acknowledgment"
            );
            case PARTIAL_FAILURE -> new GatewayPaymentResponse(
                    gatewayPaymentId,
                    PaymentStatus.FAILED,
                    PaymentStatus.CAPTURED,
                    authorizationId,
                    gatewayReference,
                    toJson(paymentResponseBody(gatewayPaymentId, request.gatewayOrderId(), "captured", request.amount(), request.currency(), gatewayReference)),
                    false,
                    true,
                    true,
                    "payment.captured",
                    webhookDelay,
                    "PARTIAL_FAILURE",
                    "Gateway captured payment but merchant update pipeline failed"
            );
            case SUCCESS_DB_NOT_UPDATED -> new GatewayPaymentResponse(
                    gatewayPaymentId,
                    PaymentStatus.FAILED,
                    PaymentStatus.CAPTURED,
                    authorizationId,
                    gatewayReference,
                    toJson(paymentResponseBody(gatewayPaymentId, request.gatewayOrderId(), "captured", request.amount(), request.currency(), gatewayReference)),
                    false,
                    true,
                    true,
                    "payment.captured",
                    webhookDelay,
                    "DB_NOT_UPDATED",
                    "Payment succeeded in gateway but local database update was intentionally skipped"
            );
        };

        log.info("gateway_payment_initiated gateway={} gatewayOrderId={} gatewayPaymentId={} scenario={} status={} gatewayFinalStatus={} timedOut={} merchantViewShouldFail={}",
                getGatewayType(),
                request.gatewayOrderId(),
                response.gatewayPaymentId(),
                scenario,
                response.status(),
                response.gatewayFinalStatus(),
                response.timedOut(),
                response.merchantViewShouldFail());
        return response;
    }

    @Override
    public GatewayStatusResponse fetchPaymentStatus(String gatewayPaymentId, String gatewayOrderId) {
        PaymentStatus status = inferStatusFromGatewayPaymentId(gatewayPaymentId);
        Map<String, Object> body = paymentResponseBody(
                gatewayPaymentId,
                gatewayOrderId,
                status.name().toLowerCase(),
                BigDecimal.ZERO,
                "INR",
                "poll_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        );
        return new GatewayStatusResponse(
                gatewayPaymentId,
                status,
                toJson(body),
                status == PaymentStatus.FAILED ? "DECLINED_BY_BANK" : null,
                status == PaymentStatus.FAILED ? "Issuer declined the transaction" : null
        );
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        return SecurityUtils.constantTimeEquals(signWebhookPayload(payload), signature);
    }

    @Override
    public String signWebhookPayload(String payload) {
        return SecurityUtils.hmacSha256Hex(properties.webhookSecret(), payload);
    }

    @Override
    public String buildWebhookEventPayload(String eventType, String gatewayEventId, PaymentOrder order, PaymentTransaction transaction) {
        Map<String, Object> paymentEntity = new LinkedHashMap<>();
        paymentEntity.put("id", transaction.getGatewayPaymentId());
        paymentEntity.put("entity", "payment");
        paymentEntity.put("amount", toMinorUnits(transaction.getAmount()));
        paymentEntity.put("currency", transaction.getCurrency());
        paymentEntity.put("status", eventType.endsWith("captured") ? "captured" : eventType.endsWith("failed") ? "failed" : transaction.getStatus().name().toLowerCase());
        paymentEntity.put("order_id", order.getGatewayOrderId());
        paymentEntity.put("method", transaction.getPaymentMethod());
        paymentEntity.put("error_code", transaction.getFailureCode());
        paymentEntity.put("error_description", transaction.getFailureReason());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("event", eventType);
        body.put("created_at", Instant.now().getEpochSecond());
        body.put("event_id", gatewayEventId);
        body.put("payload", Map.of("payment", Map.of("entity", paymentEntity)));
        return toJson(body);
    }

    private long defaultDelaySeconds(SimulationScenario scenario) {
        return switch (scenario) {
            case SUCCESS -> 5;
            case FAIL -> 2;
            case GATEWAY_TIMEOUT -> 20;
            case PARTIAL_FAILURE, SUCCESS_DB_NOT_UPDATED -> 15;
            case AUTHORIZE_ONLY -> 0;
        };
    }

    private String buildPaymentId(SimulationScenario scenario) {
        String prefix = switch (scenario) {
            case SUCCESS, GATEWAY_TIMEOUT, PARTIAL_FAILURE, SUCCESS_DB_NOT_UPDATED -> "pay_cap_";
            case AUTHORIZE_ONLY -> "pay_auth_";
            case FAIL -> "pay_fail_";
        };
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private PaymentStatus inferStatusFromGatewayPaymentId(String gatewayPaymentId) {
        if (gatewayPaymentId == null || gatewayPaymentId.isBlank()) {
            return PaymentStatus.CREATED;
        }
        if (gatewayPaymentId.startsWith("pay_fail_")) {
            return PaymentStatus.FAILED;
        }
        if (gatewayPaymentId.startsWith("pay_auth_")) {
            return PaymentStatus.AUTHORIZED;
        }
        if (gatewayPaymentId.startsWith("pay_ref_")) {
            return PaymentStatus.REFUNDED;
        }
        return PaymentStatus.CAPTURED;
    }

    private Map<String, Object> paymentResponseBody(String gatewayPaymentId, String gatewayOrderId, String status, BigDecimal amount, String currency, String gatewayReference) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", gatewayPaymentId);
        response.put("entity", "payment");
        response.put("amount", toMinorUnits(amount));
        response.put("currency", currency);
        response.put("status", status);
        response.put("order_id", gatewayOrderId);
        response.put("reference", gatewayReference);
        return response;
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new GatewayIntegrationException("Unable to serialize Razorpay payload", exception);
        }
    }
}