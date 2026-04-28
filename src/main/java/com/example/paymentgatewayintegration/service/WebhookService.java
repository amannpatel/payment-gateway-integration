package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.common.CorrelationIdFilter;
import com.example.paymentgatewayintegration.common.SecurityUtils;
import com.example.paymentgatewayintegration.dto.response.WebhookEventResponse;
import com.example.paymentgatewayintegration.dto.response.WebhookReceiptResponse;
import com.example.paymentgatewayintegration.entity.WebhookEvent;
import com.example.paymentgatewayintegration.enums.GatewayType;
import com.example.paymentgatewayintegration.enums.WebhookProcessingStatus;
import com.example.paymentgatewayintegration.exception.InvalidWebhookException;
import com.example.paymentgatewayintegration.gateway.PaymentGatewayClient;
import com.example.paymentgatewayintegration.repository.WebhookEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebhookEventRepository webhookEventRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final AsyncWebhookProcessor asyncWebhookProcessor;
    private final ObjectMapper objectMapper;

    public WebhookService(
            WebhookEventRepository webhookEventRepository,
            PaymentGatewayRegistry gatewayRegistry,
            AsyncWebhookProcessor asyncWebhookProcessor,
            ObjectMapper objectMapper
    ) {
        this.webhookEventRepository = webhookEventRepository;
        this.gatewayRegistry = gatewayRegistry;
        this.asyncWebhookProcessor = asyncWebhookProcessor;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WebhookReceiptResponse receiveRazorpayWebhook(String payload, String signature, String eventId, String deliveryId, boolean asyncProcessing) {
        PaymentGatewayClient gatewayClient = gatewayRegistry.resolve(GatewayType.RAZORPAY);
        ParsedWebhook parsedWebhook = parse(payload);
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID);
        boolean duplicateDelivery = !webhookEventRepository.findAllByGatewayEventIdOrderByReceivedAtAsc(eventId).isEmpty();
        boolean signatureValid = gatewayClient.verifyWebhookSignature(payload, signature);

        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setGateway(GatewayType.RAZORPAY);
        webhookEvent.setGatewayEventId(eventId);
        webhookEvent.setDeliveryId(deliveryId == null || deliveryId.isBlank() ? UUID.randomUUID().toString() : deliveryId);
        webhookEvent.setEventType(parsedWebhook.eventType());
        webhookEvent.setSignatureValid(signatureValid);
        webhookEvent.setPaymentId(parsedWebhook.paymentId());
        webhookEvent.setOrderId(parsedWebhook.orderId());
        webhookEvent.setPayload(payload);
        webhookEvent.setPayloadHash(SecurityUtils.sha256Hex(payload));
        webhookEvent.setProcessingStatus(signatureValid ? WebhookProcessingStatus.RECEIVED : WebhookProcessingStatus.FAILED);
        webhookEvent.setDuplicateDelivery(duplicateDelivery);
        webhookEvent.setCorrelationId(correlationId);
        webhookEvent.setReceivedAt(OffsetDateTime.now());

        WebhookEvent savedEvent = webhookEventRepository.save(webhookEvent);
        log.info("webhook_received eventId={} deliveryId={} eventType={} duplicateDelivery={} signatureValid={}", eventId, savedEvent.getDeliveryId(), parsedWebhook.eventType(), duplicateDelivery, signatureValid);

        if (!signatureValid) {
            savedEvent.setProcessingError("Webhook signature verification failed");
            webhookEventRepository.save(savedEvent);
            throw new InvalidWebhookException("Invalid Razorpay webhook signature");
        }

        if (asyncProcessing) {
            asyncWebhookProcessor.processWebhookAsync(savedEvent.getId());
        } else {
            asyncWebhookProcessor.processStoredWebhook(savedEvent.getId());
        }

        return new WebhookReceiptResponse(true, eventId, duplicateDelivery, asyncProcessing ? "ASYNC" : "SYNC", savedEvent.getReceivedAt());
    }

    public List<WebhookEventResponse> findWebhookEvents(String orderId, String paymentId) {
        List<WebhookEvent> events;
        if (paymentId != null && !paymentId.isBlank()) {
            events = webhookEventRepository.findAllByPaymentIdOrderByReceivedAtDesc(paymentId);
        } else if (orderId != null && !orderId.isBlank()) {
            events = webhookEventRepository.findAllByOrderIdOrderByReceivedAtDesc(orderId);
        } else {
            events = webhookEventRepository.findAll().stream()
                    .sorted(Comparator.comparing(WebhookEvent::getReceivedAt).reversed())
                    .toList();
        }
        return events.stream().map(this::toResponse).toList();
    }

    private WebhookEventResponse toResponse(WebhookEvent webhookEvent) {
        return new WebhookEventResponse(
                webhookEvent.getId(),
                webhookEvent.getGateway(),
                webhookEvent.getGatewayEventId(),
                webhookEvent.getDeliveryId(),
                webhookEvent.getEventType(),
                webhookEvent.isSignatureValid(),
                webhookEvent.isDuplicateDelivery(),
                webhookEvent.getProcessingStatus(),
                webhookEvent.getPaymentId(),
                webhookEvent.getOrderId(),
                webhookEvent.getProcessingError(),
                webhookEvent.getReceivedAt(),
                webhookEvent.getProcessedAt()
        );
    }

    private ParsedWebhook parse(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            return new ParsedWebhook(
                    root.path("event").asText(),
                    paymentEntity.path("id").asText(null),
                    paymentEntity.path("order_id").asText(null)
            );
        } catch (IOException exception) {
            throw new InvalidWebhookException("Unable to parse webhook payload");
        }
    }

    private record ParsedWebhook(String eventType, String paymentId, String orderId) {
    }
}