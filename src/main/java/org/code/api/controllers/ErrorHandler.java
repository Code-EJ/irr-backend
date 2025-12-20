package org.code.api.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleRequestBodyValidationError(MethodArgumentNotValidException exception) {
        var missingFields = exception.getFieldErrors().stream()
            .map(error -> error.getField())
            .toList();

        return ResponseEntity
            .badRequest()
            .body(Map.of(
                "error", "bad_request",
                "message", "Missing or invalid fields: " + String.join(", ", missingFields)
            ));
    }
}
