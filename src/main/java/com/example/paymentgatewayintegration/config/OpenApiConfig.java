package com.example.paymentgatewayintegration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentGatewayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Payment Gateway Integration Simulator")
                .description("Production-style backend for merchant payment orchestration, webhook troubleshooting, retry handling, and support workflows.")
                .version("v1")
                .contact(new Contact().name("Integration Engineering Sandbox"))
                .license(new License().name("MIT")));
    }
}