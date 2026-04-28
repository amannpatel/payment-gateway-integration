package com.example.paymentgatewayintegration.repository;

import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByGatewayPaymentId(String gatewayPaymentId);

    List<PaymentTransaction> findAllByPaymentOrderMerchantOrderIdOrderByCreatedAtAsc(String merchantOrderId);

    List<PaymentTransaction> findAllByPaymentOrderIdOrderByAttemptNumberAsc(Long paymentOrderId);

    Optional<PaymentTransaction> findTopByPaymentOrderIdOrderByAttemptNumberDesc(Long paymentOrderId);
}