package com.example.paymentgatewayintegration.repository;

import com.example.paymentgatewayintegration.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    List<WebhookEvent> findAllByGatewayEventIdOrderByReceivedAtAsc(String gatewayEventId);

    List<WebhookEvent> findAllByOrderIdOrderByReceivedAtDesc(String orderId);

    List<WebhookEvent> findAllByPaymentIdOrderByReceivedAtDesc(String paymentId);
}