package it.northleap.backend.exceptions;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

// @Order(0): vedi nota in GlobalExceptionAdvice - deve avere precedenza sul catch-all
@RestControllerAdvice
@Order(0)
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
