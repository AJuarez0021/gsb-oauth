package com.work.proxy.domain.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import static org.junit.jupiter.api.Assertions.*;

class ServiceExceptionTest {

    @Test
    void shouldCreateExceptionWithHttpStatusCode() {
        HttpStatusCode statusCode = HttpStatus.BAD_REQUEST;
        String message = "Bad request error";

        ServiceException exception = new ServiceException(statusCode, message);

        assertEquals(statusCode, exception.getStatusCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldCreateExceptionWithIntStatusCode() {
        int statusCode = 404;
        String message = "Resource not found";

        ServiceException exception = new ServiceException(statusCode, message);

        assertEquals(HttpStatusCode.valueOf(statusCode), exception.getStatusCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    void shouldReturnCorrectStatusCodeValue() {
        ServiceException exception = new ServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Server error");

        assertEquals(500, exception.getStatusCode().value());
    }

    @Test
    void shouldHandleServiceUnavailable() {
        ServiceException exception = new ServiceException(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable");

        assertEquals(503, exception.getStatusCode().value());
        assertEquals("Service unavailable", exception.getMessage());
    }

    @Test
    void shouldPreserveCauseWhenCreatedWithThrowable() {
        RuntimeException cause = new RuntimeException("original cause");
        ServiceException exception = new ServiceException(HttpStatus.BAD_GATEWAY, "wrapped error", cause);

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertEquals("wrapped error", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
