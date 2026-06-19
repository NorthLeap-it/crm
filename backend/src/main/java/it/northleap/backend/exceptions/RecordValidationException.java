package it.northleap.backend.exceptions;

public class RecordValidationException extends RuntimeException {
    public RecordValidationException(String message) {
        super(message);
    }
}
