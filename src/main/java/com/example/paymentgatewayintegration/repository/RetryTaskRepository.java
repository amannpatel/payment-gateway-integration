package com.example.paymentgatewayintegration.repository;

import com.example.paymentgatewayintegration.entity.RetryTask;
import com.example.paymentgatewayintegration.enums.RetryTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface RetryTaskRepository extends JpaRepository<RetryTask, Long> {

    List<RetryTask> findTop50ByStatusInAndNextAttemptAtBeforeOrderByNextAttemptAtAsc(Collection<RetryTaskStatus> statuses, OffsetDateTime nextAttemptAt);
}