package com.pizzaplatform.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class of all business errors.
 * Each subclass maps to one HTTP status, so the services stay independent from the web layer
 * and {@link GlobalExceptionHandler} turns them into RFC 7807 "problem details" responses.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
