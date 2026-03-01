package com.work.proxy.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ServiceException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public ServiceException(HttpStatusCode statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ServiceException(int statusCode, String message) {
        super(message);
        this.statusCode = HttpStatusCode.valueOf(statusCode);
    }

    public ServiceException(HttpStatusCode statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
