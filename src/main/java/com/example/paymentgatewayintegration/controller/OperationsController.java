package com.example.paymentgatewayintegration.controller;

import com.example.paymentgatewayintegration.dto.request.SimulateDelayedWebhookRequest;
import com.example.paymentgatewayintegration.dto.request.SimulateDuplicateWebhookRequest;
import com.example.paymentgatewayintegration.dto.request.SimulateStatusDriftRequest;
import com.example.paymentgatewayintegration.dto.response.RetryTaskResponse;
import com.example.paymentgatewayintegration.dto.response.SimulationActionResponse;
import com.example.paymentgatewayintegration.dto.response.TransactionDiagnosticsResponse;
import com.example.paymentgatewayintegration.dto.response.WebhookEventResponse;
import com.example.paymentgatewayintegration.service.SimulationService;
import com.example.paymentgatewayintegration.service.TroubleshootingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/internal")
public class OperationsController {

    private final TroubleshootingService troubleshootingService;
    private final SimulationService simulationService;

    public OperationsController(TroubleshootingService troubleshootingService, SimulationService simulationService) {
        this.troubleshootingService = troubleshootingService;
        this.simulationService = simulationService;
    }

    @GetMapping("/transactions/search")
    public TransactionDiagnosticsResponse searchTransactions(
            @RequestParam(required = false) String merchantOrderId,
            @RequestParam(required = false) String paymentId
    ) {
        return troubleshootingService.searchTransactions(merchantOrderId, paymentId);
    }

    @GetMapping("/orders/{merchantOrderId}/history")
    public TransactionDiagnosticsResponse getHistory(@PathVariable String merchantOrderId) {
        return troubleshootingService.searchTransactions(merchantOrderId, null);
    }

    @GetMapping("/webhooks")
    public List<WebhookEventResponse> getWebhookLogs(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String paymentId
    ) {
        return troubleshootingService.getWebhookLogs(orderId, paymentId);
    }

    @GetMapping("/retries")
    public List<RetryTaskResponse> getRetries() {
        return troubleshootingService.listRetryTasks();
    }

    @PostMapping("/simulations/duplicate-webhook")
    public SimulationActionResponse simulateDuplicateWebhook(@Valid @RequestBody SimulateDuplicateWebhookRequest request) {
        return simulationService.simulateDuplicateWebhook(request);
    }

    @PostMapping("/simulations/delayed-webhook")
    public SimulationActionResponse simulateDelayedWebhook(@Valid @RequestBody SimulateDelayedWebhookRequest request) {
        return simulationService.simulateDelayedWebhook(request);
    }

    @PostMapping("/simulations/payment-success-db-miss")
    public SimulationActionResponse simulatePaymentSuccessDbMiss(@Valid @RequestBody SimulateStatusDriftRequest request) {
        return simulationService.simulatePaymentSuccessDbMiss(request);
    }
}