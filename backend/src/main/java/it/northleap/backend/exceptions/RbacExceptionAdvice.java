package it.northleap.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class RbacExceptionAdvice {

    @ExceptionHandler(RbacDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleRbacDenied(RbacDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of(
                        "timestamp", Instant.now().toString(),
                        "status", 403,
                        "message", ex.getMessage()
                )
        );
    }
}
