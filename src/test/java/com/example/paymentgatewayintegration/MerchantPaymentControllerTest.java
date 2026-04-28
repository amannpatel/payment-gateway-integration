package com.example.paymentgatewayintegration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MerchantPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateOrderAndInitiateAuthorizedPayment() throws Exception {
        mockMvc.perform(post("/api/v1/payments/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchantId": "merchant-demo",
                                  "merchantOrderId": "ORD-1001",
                                  "gateway": "RAZORPAY",
                                  "amount": 1499.99,
                                  "currency": "INR",
                                  "description": "Integration sandbox order",
                                  "customerEmail": "buyer@example.com",
                                  "customerPhone": "+919999999999"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.gatewayOrderId").exists());

        mockMvc.perform(post("/api/v1/payments/orders/ORD-1001/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "card",
                                  "scenario": "AUTHORIZE_ONLY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantVisibleStatus").value("AUTHORIZED"))
                .andExpect(jsonPath("$.gatewayPaymentId").exists());

        mockMvc.perform(get("/api/v1/payments/orders/ORD-1001/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.attempts[0].attemptNumber").value(1));
    }
}