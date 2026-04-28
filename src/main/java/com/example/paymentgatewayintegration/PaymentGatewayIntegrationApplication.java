package com.example.paymentgatewayintegration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableRetry
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class PaymentGatewayIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayIntegrationApplication.class, args);
    }
}