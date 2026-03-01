package com.work.proxy.infrastructure.adapter.in.rest;

import com.work.proxy.application.dto.Error;
import com.work.proxy.domain.exception.ServiceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<Error>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        Error errorResponse = Error.builder()
                .error(message)
                .status(400)
                .timestamp(Instant.now())
                .build();
        return Mono.just(ResponseEntity.badRequest().body(errorResponse));
    }

    @ExceptionHandler(ServiceException.class)
    public Mono<ResponseEntity<Error>> handleServiceException(ServiceException ex) {
        log.error("Service exception occurred: {} - Status: {}", ex.getMessage(), ex.getStatusCode());

        Error errorResponse = Error.builder()
                .error(ex.getMessage())
                .status(ex.getStatusCode().value())
                .timestamp(Instant.now())
                .build();
        return Mono.just(ResponseEntity
                .status(ex.getStatusCode())
                .body(errorResponse));
    }


    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Error>> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        Error errorResponse = Error.builder()
                .error("An unexpected error occurred")
                .status(500)
                .timestamp(Instant.now())
                .build();
        return Mono.just(ResponseEntity
                .internalServerError()
                .body(errorResponse));
    }
}
