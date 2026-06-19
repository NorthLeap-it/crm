package it.northleap.backend.exceptions;

public class RbacDeniedException extends RuntimeException {
    public RbacDeniedException() {
        super("Forbidden");
    }

    public RbacDeniedException(String message) {
        super(message);
    }
}
