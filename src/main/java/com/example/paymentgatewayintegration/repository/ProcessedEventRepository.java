package com.example.paymentgatewayintegration.repository;

import com.example.paymentgatewayintegration.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventKey(String eventKey);

    Optional<ProcessedEvent> findByEventKey(String eventKey);
}