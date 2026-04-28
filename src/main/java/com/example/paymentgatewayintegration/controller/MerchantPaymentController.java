package com.example.paymentgatewayintegration.controller;

import com.example.paymentgatewayintegration.dto.request.CreatePaymentOrderRequest;
import com.example.paymentgatewayintegration.dto.request.InitiatePaymentRequest;
import com.example.paymentgatewayintegration.dto.response.PaymentInitiationResponse;
import com.example.paymentgatewayintegration.dto.response.PaymentOrderResponse;
import com.example.paymentgatewayintegration.dto.response.PaymentStatusResponse;
import com.example.paymentgatewayintegration.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class MerchantPaymentController {

    private final PaymentService paymentService;

    public MerchantPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentOrderResponse createOrder(@Valid @RequestBody CreatePaymentOrderRequest request) {
        return paymentService.createOrder(request);
    }

    @PostMapping("/orders/{merchantOrderId}/initiate")
    public PaymentInitiationResponse initiatePayment(
            @PathVariable String merchantOrderId,
            @Valid @RequestBody InitiatePaymentRequest request
    ) {
        return paymentService.initiatePayment(merchantOrderId, request);
    }

    @GetMapping("/orders/{merchantOrderId}/status")
    public PaymentStatusResponse getStatus(@PathVariable String merchantOrderId) {
        return paymentService.getPaymentStatus(merchantOrderId);
    }
}