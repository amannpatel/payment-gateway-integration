package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.entity.PaymentOrder;
import com.example.paymentgatewayintegration.entity.PaymentStatusHistory;
import com.example.paymentgatewayintegration.entity.PaymentTransaction;
import com.example.paymentgatewayintegration.enums.PaymentEventSource;
import com.example.paymentgatewayintegration.enums.PaymentStatus;
import com.example.paymentgatewayintegration.exception.InvalidStateTransitionException;
import com.example.paymentgatewayintegration.repository.PaymentOrderRepository;
import com.example.paymentgatewayintegration.repository.PaymentStatusHistoryRepository;
import com.example.paymentgatewayintegration.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;

@Service
public class PaymentLifecycleService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;

    public PaymentLifecycleService(
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentOrderRepository paymentOrderRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository
    ) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentOrderRepository = paymentOrderRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
    }

    @Transactional
    public PaymentTransaction transition(
            PaymentTransaction transaction,
            PaymentStatus targetStatus,
            PaymentEventSource source,
            String sourceReference,
            String notes
    ) {
        PaymentStatus currentStatus = transaction.getStatus();
        if (currentStatus == targetStatus) {
            return transaction;
        }
        if (!isAllowed(currentStatus, targetStatus, source)) {
            throw new InvalidStateTransitionException("Transition %s -> %s is not allowed for source %s".formatted(currentStatus, targetStatus, source));
        }

        transaction.setStatus(targetStatus);
        applyStatusMetadata(transaction, targetStatus);

        PaymentOrder paymentOrder = transaction.getPaymentOrder();
        paymentOrder.setStatus(targetStatus);
        paymentOrder.setLatestPaymentId(transaction.getGatewayPaymentId());
        if (targetStatus == PaymentStatus.FAILED) {
            paymentOrder.setFailureCode(transaction.getFailureCode());
            paymentOrder.setFailureReason(transaction.getFailureReason());
        } else {
            paymentOrder.setFailureCode(null);
            paymentOrder.setFailureReason(null);
        }

        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPaymentTransaction(transaction);
        history.setFromStatus(currentStatus);
        history.setToStatus(targetStatus);
        history.setSource(source);
        history.setSourceReference(sourceReference);
        history.setNotes(notes);

        paymentOrderRepository.save(paymentOrder);
        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);
        paymentStatusHistoryRepository.save(history);
        return savedTransaction;
    }

    private void applyStatusMetadata(PaymentTransaction transaction, PaymentStatus targetStatus) {
        OffsetDateTime now = OffsetDateTime.now();
        switch (targetStatus) {
            case AUTHORIZED -> {
                if (transaction.getAuthorizedAt() == null) {
                    transaction.setAuthorizedAt(now);
                }
            }
            case CAPTURED -> {
                if (transaction.getAuthorizedAt() == null) {
                    transaction.setAuthorizedAt(now);
                }
                if (transaction.getCapturedAt() == null) {
                    transaction.setCapturedAt(now);
                }
                transaction.setFailureCode(null);
                transaction.setFailureReason(null);
            }
            case REFUNDED -> {
                if (transaction.getRefundedAt() == null) {
                    transaction.setRefundedAt(now);
                }
            }
            case FAILED, CREATED -> {
            }
        }
    }

    private boolean isAllowed(PaymentStatus current, PaymentStatus target, PaymentEventSource source) {
        if (current == null) {
            return true;
        }
        if (EnumSet.of(PaymentEventSource.RETRY, PaymentEventSource.SIMULATION, PaymentEventSource.SUPPORT_TOOL, PaymentEventSource.GATEWAY_POLL).contains(source)) {
            return true;
        }
        return switch (current) {
            case CREATED -> EnumSet.of(PaymentStatus.AUTHORIZED, PaymentStatus.CAPTURED, PaymentStatus.FAILED).contains(target);
            case AUTHORIZED -> EnumSet.of(PaymentStatus.CAPTURED, PaymentStatus.FAILED, PaymentStatus.REFUNDED).contains(target);
            case CAPTURED -> target == PaymentStatus.REFUNDED;
            case FAILED -> false;
            case REFUNDED -> false;
        };
    }
}