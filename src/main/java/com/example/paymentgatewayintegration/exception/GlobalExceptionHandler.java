package com.example.paymentgatewayintegration.exception;

import com.example.paymentgatewayintegration.common.CorrelationIdFilter;
import com.example.paymentgatewayintegration.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler({DuplicateRequestException.class, InvalidStateTransitionException.class})
    public ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException exception, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request, null);
    }

    @ExceptionHandler({UnsupportedGatewayException.class, InvalidWebhookException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException exception, HttpServletRequest request) {
        HttpStatus status = exception instanceof InvalidWebhookException ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
        return build(status, exception.getMessage(), request, null);
    }

    @ExceptionHandler(GatewayIntegrationException.class)
    public ResponseEntity<ApiErrorResponse> handleGateway(GatewayIntegrationException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, exception.getMessage(), request, exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error", request, exception);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest request, Exception exception) {
        if (exception != null) {
            log.error("request_failed path={} status={} message={}", request.getRequestURI(), status.value(), message, exception);
        }
        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                MDC.get(CorrelationIdFilter.CORRELATION_ID)
        );
        return ResponseEntity.status(status).body(body);
    }
}