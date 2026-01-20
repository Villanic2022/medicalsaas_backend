package com.saas.medical.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s no encontrado con %s: '%s'", resource, field, value));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
