package com.work.proxy.infrastructure.adapter.in.rest;

import com.work.proxy.domain.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.work.proxy.application.dto.Error;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleServiceException() {
        ServiceException exception = new ServiceException(HttpStatus.BAD_REQUEST, "Invalid request");

        Mono<ResponseEntity<Error>> result = handler.handleServiceException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                    Error body = response.getBody();
                    assertNotNull(body);
                    assertEquals(400, body.getStatus());
                    assertEquals("Invalid request", body.getError());
                    assertNotNull(body.getTimestamp());
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleServiceExceptionWithDifferentStatus() {
        ServiceException exception = new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, "OAuth service down");

        Mono<ResponseEntity<Error>> result = handler.handleServiceException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
                    Error body = response.getBody();
                    assertNotNull(body);
                    assertEquals(503, body.getStatus());
                    assertEquals("OAuth service down", body.getError());
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleGenericException() {
        Exception exception = new RuntimeException("Unexpected error occurred");

        Mono<ResponseEntity<Error>> result = handler.handleGenericException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                    Error body = response.getBody();
                    assertNotNull(body);
                    assertEquals(500, body.getStatus());
                    assertEquals("An unexpected error occurred", body.getError());
                })
                .verifyComplete();
    }

    @Test
    void shouldIncludeTimestampInServiceExceptionResponse() {
        ServiceException exception = new ServiceException(HttpStatus.NOT_FOUND, "Not found");

        Mono<ResponseEntity<Error>> result = handler.handleServiceException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    Error body = response.getBody();
                    assertNotNull(body);
                    String timestamp = body.getTimestamp().toString();
                    assertNotNull(timestamp);
                    assertFalse(timestamp.isEmpty());
                    assertTrue(timestamp.contains("T"));
                })
                .verifyComplete();
    }

    @Test
    void shouldHandleTimeoutException() {
        ServiceException exception = new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Timeout en OAuth");

        Mono<ResponseEntity<Error>> result = handler.handleServiceException(exception);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
                    Error body = response.getBody();
                    assertNotNull(body);
                    assertEquals("Timeout en OAuth", body.getError());
                })
                .verifyComplete();
    }
}
