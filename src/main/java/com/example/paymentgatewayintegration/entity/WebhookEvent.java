package com.example.paymentgatewayintegration.entity;

import com.example.paymentgatewayintegration.common.AuditableEntity;
import com.example.paymentgatewayintegration.enums.GatewayType;
import com.example.paymentgatewayintegration.enums.WebhookProcessingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", nullable = false, length = 32)
    private GatewayType gateway;

    @Column(name = "gateway_event_id", nullable = false, length = 128)
    private String gatewayEventId;

    @Column(name = "delivery_id", nullable = false, length = 128)
    private String deliveryId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(name = "payment_id", length = 128)
    private String paymentId;

    @Column(name = "order_id", length = 128)
    private String orderId;

    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private WebhookProcessingStatus processingStatus;

    @Column(name = "processing_error", length = 1024)
    private String processingError;

    @Column(name = "duplicate_delivery", nullable = false)
    private boolean duplicateDelivery;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public Long getId() {
        return id;
    }

    public GatewayType getGateway() {
        return gateway;
    }

    public void setGateway(GatewayType gateway) {
        this.gateway = gateway;
    }

    public String getGatewayEventId() {
        return gatewayEventId;
    }

    public void setGatewayEventId(String gatewayEventId) {
        this.gatewayEventId = gatewayEventId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public void setSignatureValid(boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(String payloadHash) {
        this.payloadHash = payloadHash;
    }

    public WebhookProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(WebhookProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getProcessingError() {
        return processingError;
    }

    public void setProcessingError(String processingError) {
        this.processingError = processingError;
    }

    public boolean isDuplicateDelivery() {
        return duplicateDelivery;
    }

    public void setDuplicateDelivery(boolean duplicateDelivery) {
        this.duplicateDelivery = duplicateDelivery;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}