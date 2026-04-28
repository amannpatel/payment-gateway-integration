package com.example.paymentgatewayintegration.service;

import com.example.paymentgatewayintegration.entity.ProcessedEvent;
import com.example.paymentgatewayintegration.repository.ProcessedEventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    public IdempotencyService(ProcessedEventRepository processedEventRepository) {
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    public boolean recordProcessedEvent(String eventKey, String eventType, String resourceType, String resourceId, String payloadHash, String correlationId) {
        ProcessedEvent processedEvent = new ProcessedEvent();
        processedEvent.setEventKey(eventKey);
        processedEvent.setEventType(eventType);
        processedEvent.setResourceType(resourceType);
        processedEvent.setResourceId(resourceId);
        processedEvent.setPayloadHash(payloadHash);
        processedEvent.setProcessedAt(OffsetDateTime.now());
        processedEvent.setCorrelationId(correlationId);
        try {
            processedEventRepository.save(processedEvent);
            return true;
        } catch (DataIntegrityViolationException exception) {
            return false;
        }
    }
}