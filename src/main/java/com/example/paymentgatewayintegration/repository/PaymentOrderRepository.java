package com.example.paymentgatewayintegration.repository;

import com.example.paymentgatewayintegration.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    Optional<PaymentOrder> findByMerchantIdAndMerchantOrderId(String merchantId, String merchantOrderId);

    Optional<PaymentOrder> findByMerchantOrderId(String merchantOrderId);

    Optional<PaymentOrder> findByLatestPaymentId(String latestPaymentId);

    Optional<PaymentOrder> findByGatewayOrderId(String gatewayOrderId);
}