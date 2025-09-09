package com.example.bankcards.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String resourceName, String field1Name, Object field1Value, String field2Name, Object field2Value) {
        super(String.format("%s not found with %s: '%s' and %s: '%s'",
                resourceName, field1Name, field1Value, field2Name, field2Value));
    }
}
