package org.code.api.controllers;

import java.util.Map;

import org.code.api.domain.exception.AuthError;
import org.code.api.domain.exception.MaterialError;
import org.code.api.domain.exception.VehicleError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.code.api.domain.exception.DonorError;

import lombok.extern.slf4j.Slf4j;

/**
 * Handler global de exceções. Traduz exceções de domínio em respostas HTTP padronizadas.
 */
@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    // ────────────────────────────────────────────────────────────────────────────
    // Validação de payload (Jakarta Validation)
    // ────────────────────────────────────────────────────────────────────────────

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

    // ────────────────────────────────────────────────────────────────────────────
    // Spring Security — @PreAuthorize denial
    // ────────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException exception) {
        log.debug("Access denied: {}", exception.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "error", "access_denied",
                "message", "You do not have permission to perform this action."
            ));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Auth Errors
    // ────────────────────────────────────────────────────────────────────────────

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
        log.debug(
            "Expired token used, issued at: {}, expires at: {}",
            exception.getIssuedAt(),
            exception.getExpiresAt()
        );

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(Map.of(
                "error", "expired_token",
                "message", "The provided token has expired.",
                "expiration_time", exception.getExpiresAt().getEpochSecond(),
                "issued_at", exception.getIssuedAt().getEpochSecond()
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

    // ────────────────────────────────────────────────────────────────────────────
    // Vehicle Errors
    // ────────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(VehicleError.NotFound.class)
    public ResponseEntity<?> handleVehicleNotFound(VehicleError.NotFound exception) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "vehicle_not_found",
                "message", "Vehicle not found",
                "vehicle_id", exception.getVehicleId().toString()
            ));
    }

    @ExceptionHandler(VehicleError.PlateAlreadyExists.class)
    public ResponseEntity<?> handleVehiclePlateAlreadyExists(
        VehicleError.PlateAlreadyExists exception
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error", "vehicle_plate_occupied",
                "message", "Vehicle plate is already in use",
                "license_plate", exception.getLicensePlate()
            ));
    }

    @ExceptionHandler(VehicleError.InactiveVehicle.class)
    public ResponseEntity<?> handleInactiveVehicle(VehicleError.InactiveVehicle exception) {
        return ResponseEntity
            .unprocessableEntity()
            .body(Map.of(
                "error", "inactive_vehicle",
                "message", exception.getMessage()
            ));
    }

    @ExceptionHandler(VehicleError.HasCollectionBinding.class)
    public ResponseEntity<?> handleVehicleHasCollectionBinding(
        VehicleError.HasCollectionBinding exception
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error", "vehicle_has_collection_binding",
                "message", exception.getMessage(),
                "vehicle_id", exception.getVehicleId().toString()
            ));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Donor Errors
    // ────────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(DonorError.NotFound.class)
    public ResponseEntity<?> handleDonorNotFound(DonorError.NotFound exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", "donor_not_found",
                        "message", "Donor not found",
                        "donor_id", exception.getDonorId().toString()
                ));
    }

    @ExceptionHandler(DonorError.DocumentAlreadyExists.class)
    public ResponseEntity<?> handleDonorDocumentAlreadyExists(DonorError.DocumentAlreadyExists exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", "donor_document_occupied",
                        "message", "Donor document is already in use",
                        "document", exception.getDocument()
                ));
    }

    @ExceptionHandler(DonorError.InactiveDonor.class)
    public ResponseEntity<?> handleInactiveDonor(DonorError.InactiveDonor exception) {
        return ResponseEntity
                .unprocessableEntity()
                .body(Map.of(
                        "error", "inactive_donor",
                        "message", exception.getMessage()
                ));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Material Errors
    // ────────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(MaterialError.NotFound.class)
    public ResponseEntity<?> handleMaterialNotFound(MaterialError.NotFound exception) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "material_not_found",
                "message", exception.getMessage(),
                "material_id", exception.getMaterialId().toString(),
                "level", exception.getLevel()
            ));
    }

    @ExceptionHandler(MaterialError.ParentNotFound.class)
    public ResponseEntity<?> handleMaterialParentNotFound(MaterialError.ParentNotFound exception) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "error", "material_parent_not_found",
                "message", exception.getMessage(),
                "parent_id", exception.getParentId().toString(),
                "parent_level", exception.getParentLevel()
            ));
    }

    @ExceptionHandler(MaterialError.NameAlreadyExists.class)
    public ResponseEntity<?> handleMaterialNameConflict(MaterialError.NameAlreadyExists exception) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error", "material_name_occupied",
                "message", exception.getMessage(),
                "name", exception.getName(),
                "level", exception.getLevel()
            ));
    }

    @ExceptionHandler(MaterialError.HasInventoryBinding.class)
    public ResponseEntity<?> handleMaterialInventoryBinding(MaterialError.HasInventoryBinding exception) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error", "material_has_inventory_binding",
                "message", exception.getMessage(),
                "material_id", exception.getMaterialId().toString(),
                "level", exception.getLevel()
            ));
    }

    @ExceptionHandler(MaterialError.InactiveMaterial.class)
    public ResponseEntity<?> handleInactiveMaterial(MaterialError.InactiveMaterial exception) {
        return ResponseEntity
            .unprocessableEntity()
            .body(Map.of(
                "error", "inactive_material",
                "message", exception.getMessage(),
                "material_id", exception.getMaterialId().toString(),
                "level", exception.getLevel()
            ));
    }

    @ExceptionHandler(MaterialError.ConcurrentModification.class)
    public ResponseEntity<?> handleConcurrentModification(MaterialError.ConcurrentModification exception) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error", "concurrent_modification",
                "message", exception.getMessage(),
                "material_id", exception.getMaterialId().toString()
            ));
    }
}
