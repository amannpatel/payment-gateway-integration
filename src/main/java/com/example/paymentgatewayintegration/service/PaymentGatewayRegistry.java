package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.enums.GatewayType;
import com.example.paymentgatewayintegration.exception.UnsupportedGatewayException;
import com.example.paymentgatewayintegration.gateway.PaymentGatewayClient;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentGatewayRegistry {

    private final Map<GatewayType, PaymentGatewayClient> gateways;

    public PaymentGatewayRegistry(List<PaymentGatewayClient> gatewayClients) {
        this.gateways = new EnumMap<>(GatewayType.class);
        gatewayClients.forEach(client -> this.gateways.put(client.getGatewayType(), client));
    }

    public PaymentGatewayClient resolve(GatewayType gatewayType) {
        PaymentGatewayClient client = gateways.get(gatewayType);
        if (client == null) {
            throw new UnsupportedGatewayException("Gateway %s is not implemented in this sandbox".formatted(gatewayType));
        }
        return client;
    }
}