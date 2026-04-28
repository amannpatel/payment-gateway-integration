package com.example.paymentgatewayintegration.gateway;

import com.example.paymentgatewayintegration.entity.PaymentOrder;
import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.enums.GatewayType;

public interface PaymentGatewayClient {

    GatewayType getGatewayType();

    GatewayOrderResponse createOrder(GatewayOrderRequest request);

    GatewayPaymentResponse initiatePayment(GatewayPaymentRequest request);

    GatewayStatusResponse fetchPaymentStatus(String gatewayPaymentId, String gatewayOrderId);

    boolean verifyWebhookSignature(String payload, String signature);

    String signWebhookPayload(String payload);

    String buildWebhookEventPayload(String eventType, String gatewayEventId, PaymentOrder order, PaymentTransaction transaction);
}