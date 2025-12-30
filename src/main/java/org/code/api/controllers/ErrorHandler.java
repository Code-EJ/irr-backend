package org.code.api.controllers;

import java.util.Date;
import java.util.Map;

import org.code.api.domain.exception.AuthError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

    @ExceptionHandler(AuthError.CreatorUserInvalid.class)
    public ResponseEntity<?> handleCreatorUserInvalid(AuthError.CreatorUserInvalid exception) {
        String errorCode = exception.isInvalidUUID() ? "invalid_creator_uuid" : "creator_user_not_found";

        String errorMessage = exception.isInvalidUUID()
            ? "The provided creator user ID is not a valid UUID"
            : "No user found with the provided creator user ID";

        log.debug("{} - {} - {}", errorCode, exception.getCreatorUserId(), errorMessage);

        return ResponseEntity
            .badRequest()
            .body(Map.of(
                "error", errorCode,
                "creator_user_id", exception.getCreatorUserId(),
                "message", errorMessage
            ));
    }

    @ExceptionHandler(AuthError.EmailOccupied.class)
    public ResponseEntity<?> handleEmailOccupied(AuthError.EmailOccupied exception) {
        log.debug("Attempt to register an email that is already occupied {}", exception.getEmail());

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error", "email_occupied",
                "message", "The email address is already in use: " + exception.getEmail(),
                "email", exception.getEmail()
            ));
    }

    @ExceptionHandler(AuthError.InvalidToken.class)
    public ResponseEntity<?> handleInvalidToken(AuthError.InvalidToken exception) {
        log.debug("Invalid token used: {}", exception.getToken());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "error", "invalid_token",
                "message", "The provided token is invalid.",
                "token", exception.getToken()
            ));
    }

    @ExceptionHandler(AuthError.ExpiredToken.class)
    public ResponseEntity<?> handleExpiredToken(AuthError.ExpiredToken exception) {
        log.debug("Expired token used, issued at: {}, expires at: {}", 
            new Date(exception.getIssuedAt()), 
            new Date(exception.getExpiresAt())
        );

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "error", "expired_token",
                "message", "The provided token has expired.",
                "expiration_time", exception.getExpiresAt(),
                "issued_at", exception.getIssuedAt()
            ));
    }

    @ExceptionHandler(AuthError.WrongCredentials.class)
    public ResponseEntity<?> handleWrongCredentials(AuthError.WrongCredentials exception) {
        log.debug(exception.isUserValid()
            ? "Password mismatch for email: " + exception.getEmail()
            : "No user found with email: " + exception.getEmail()
        );

        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of(
                "error", "wrong_credentials",
                "message", "The provided email or password is incorrect.",
                "email", exception.getEmail()
            ));
    }

    @ExceptionHandler(AuthError.PasswordTooLong.class)
    public ResponseEntity<?> handlePasswordTooLong(AuthError.PasswordTooLong exception) {
        log.debug("Attempt to register with a password exceeding 72 bytes: {} bytes", exception.getPasswordLength());

        return ResponseEntity
            .badRequest()
            .body(Map.of(
                "error", "password_too_long",
                "message", "The provided password exceeds the maximum allowed length of 72 bytes.",
                "password_length", exception.getPasswordLength()
            ));
    }
}
