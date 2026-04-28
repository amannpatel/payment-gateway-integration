package com.example.paymentgatewayintegration.gateway.razorpay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.razorpay")
public record RazorpayProperties(
        String keyId,
        String keySecret,
        String webhookSecret,
        String baseUrl,
        boolean mockEnabled
) {
}