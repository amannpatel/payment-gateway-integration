package com.example.paymentgatewayintegration.service;

record PaymentReconciliationPayload(String gatewayPaymentId, String gatewayOrderId, String reason) {
}

record WebhookDispatchPayload(Long transactionId, String eventType, String gatewayEventId) {
}