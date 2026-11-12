package com.pizzaplatform.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Concrete business exceptions, one per HTTP status used by the API.
 */
public final class Exceptions {

    private Exceptions() {
    }

    /** 400 – the request is syntactically valid but makes no sense for the business rules. */
    public static class InvalidRequestException extends ApiException {
        public InvalidRequestException(String message) {
            super(HttpStatus.BAD_REQUEST, message);
        }
    }

    /** 401 – missing, invalid or revoked credentials. */
    public static class UnauthorizedException extends ApiException {
        public UnauthorizedException(String message) {
            super(HttpStatus.UNAUTHORIZED, message);
        }
    }

    /** 402 – the amount paid does not cover the order price. */
    public static class InsufficientPaymentException extends ApiException {
        public InsufficientPaymentException(String message) {
            super(HttpStatus.PAYMENT_REQUIRED, message);
        }
    }

    /** 403 – the authenticated customer is not allowed to access this resource. */
    public static class ForbiddenException extends ApiException {
        public ForbiddenException(String message) {
            super(HttpStatus.FORBIDDEN, message);
        }
    }

    /** 404 – the resource does not exist. */
    public static class NotFoundException extends ApiException {
        public NotFoundException(String message) {
            super(HttpStatus.NOT_FOUND, message);
        }
    }

    /** 409 – the request conflicts with the current state of the resource. */
    public static class ConflictException extends ApiException {
        public ConflictException(String message) {
            super(HttpStatus.CONFLICT, message);
        }
    }

    /** 410 – the resource existed but has been deleted. */
    public static class GoneException extends ApiException {
        public GoneException(String message) {
            super(HttpStatus.GONE, message);
        }
    }
}
