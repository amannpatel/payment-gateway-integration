package com.example.paymentgatewayintegration.repository;

import com.example.paymentgatewayintegration.entity.MerchantIssue;
import com.example.paymentgatewayintegration.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface MerchantIssueRepository extends JpaRepository<MerchantIssue, Long> {

    List<MerchantIssue> findAllByStatusOrderByCreatedAtDesc(IssueStatus status);

    List<MerchantIssue> findAllByCreatedAtBetweenOrderByCreatedAtDesc(OffsetDateTime from, OffsetDateTime to);
}