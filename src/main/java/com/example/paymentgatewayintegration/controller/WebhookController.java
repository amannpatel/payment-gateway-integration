package com.example.paymentgatewayintegration.controller;

import com.example.paymentgatewayintegration.dto.response.WebhookReceiptResponse;
import com.example.paymentgatewayintegration.service.WebhookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookService webhookService;
    private final boolean asyncProcessing;

    public WebhookController(
            WebhookService webhookService,
            @Value("${app.webhooks.async-processing:true}") boolean asyncProcessing
    ) {
        this.webhookService = webhookService;
        this.asyncProcessing = asyncProcessing;
    }

    @PostMapping("/razorpay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public WebhookReceiptResponse receiveRazorpayWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestHeader("X-Razorpay-Event-Id") String eventId,
            @RequestHeader(value = "X-Razorpay-Delivery-Id", required = false) String deliveryId,
            @RequestBody String payload
    ) {
        return webhookService.receiveRazorpayWebhook(payload, signature, eventId, deliveryId, asyncProcessing);
    }
}