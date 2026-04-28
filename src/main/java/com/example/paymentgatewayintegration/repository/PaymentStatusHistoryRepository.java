package com.example.paymentgatewayintegration.repository;

import com.example.paymentgatewayintegration.entity.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {

    List<PaymentStatusHistory> findAllByPaymentTransactionIdOrderByCreatedAtAsc(Long paymentTransactionId);
}