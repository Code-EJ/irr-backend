package org.code.api.controllers;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.code.api.domain.exception.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates domain exceptions into the current HTTP error responses.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Slf4j
@RestControllerAdvice
public class ErrorHandler {

  /**
   * Validates request payloads using Jakarta Validation
   *
   * @param exception
   * @return
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleRequestBodyValidationError(
      MethodArgumentNotValidException exception) {
    var missingFields = exception.getFieldErrors().stream().map(error -> error.getField()).toList();

    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error",
                "bad_request",
                "message",
                "Missing or invalid fields: " + String.join(", ", missingFields)));
  }

  /**
   * Spring Security — @PreAuthorize denial
   *
   * @param exception
   * @return
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<?> handleAccessDenied(AccessDeniedException exception) {
    log.debug("Access denied: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            Map.of(
                "error",
                "access_denied",
                "message",
                "You do not have permission to perform this action."));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(AuthError.CreatorUserInvalid.class)
  public ResponseEntity<?> handleCreatorUserInvalid(AuthError.CreatorUserInvalid exception) {
    String errorCode =
        exception.isInvalidUUID() ? "invalid_creator_uuid" : "creator_user_not_found";

    String errorMessage =
        exception.isInvalidUUID()
            ? "The provided creator user ID is not a valid UUID"
            : "No user found with the provided creator user ID";

    log.debug("{} - {} - {}", errorCode, exception.getCreatorUserId(), errorMessage);

    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error", errorCode,
                "creator_user_id", exception.getCreatorUserId(),
                "message", errorMessage));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(AuthError.EmailOccupied.class)
  public ResponseEntity<?> handleEmailOccupied(AuthError.EmailOccupied exception) {
    log.debug("Attempt to register an email that is already occupied {}", exception.getEmail());

    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error",
                "email_occupied",
                "message",
                "The email address is already in use: " + exception.getEmail(),
                "email",
                exception.getEmail()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(AuthError.InvalidToken.class)
  public ResponseEntity<?> handleInvalidToken(AuthError.InvalidToken exception) {
    log.debug("Rejected an invalid token");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            Map.of(
                "error", "invalid_token",
                "message", "The provided token is invalid."));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(AuthError.ExpiredToken.class)
  public ResponseEntity<?> handleExpiredToken(AuthError.ExpiredToken exception) {
    log.debug(
        "Expired token used, issued at: {}, expires at: {}",
        exception.getIssuedAt(),
        exception.getExpiresAt());

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(
            Map.of(
                "error",
                "expired_token",
                "message",
                "The provided token has expired.",
                "expiration_time",
                exception.getExpiresAt().getEpochSecond(),
                "issued_at",
                exception.getIssuedAt().getEpochSecond()));
  }

  /**
   * Uses the same response for unknown accounts and wrong passwords.
   *
   * @param exception rejected credentials
   * @return a generic unauthorized response
   */
  @ExceptionHandler(AuthError.WrongCredentials.class)
  public ResponseEntity<?> handleWrongCredentials(AuthError.WrongCredentials exception) {
    return ResponseEntity.status(401)
        .body(Map.of("error", "wrong_credentials", "message", "Invalid email or password"));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(VehicleError.NotFound.class)
  public ResponseEntity<?> handleVehicleNotFound(VehicleError.NotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "vehicle_not_found",
                "message", "Vehicle not found",
                "vehicle_id", exception.getVehicleId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(VehicleError.PlateAlreadyExists.class)
  public ResponseEntity<?> handleVehiclePlateAlreadyExists(
      VehicleError.PlateAlreadyExists exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error", "vehicle_plate_occupied",
                "message", "Vehicle plate is already in use",
                "license_plate", exception.getLicensePlate()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(VehicleError.InactiveVehicle.class)
  public ResponseEntity<?> handleInactiveVehicle(VehicleError.InactiveVehicle exception) {
    return ResponseEntity.unprocessableEntity()
        .body(Map.of("error", "inactive_vehicle", "message", exception.getMessage()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(VehicleError.HasCollectionBinding.class)
  public ResponseEntity<?> handleVehicleHasCollectionBinding(
      VehicleError.HasCollectionBinding exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error", "vehicle_has_collection_binding",
                "message", exception.getMessage(),
                "vehicle_id", exception.getVehicleId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(DonorError.NotFound.class)
  public ResponseEntity<?> handleDonorNotFound(DonorError.NotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "donor_not_found",
                "message", "Donor not found",
                "donor_id", exception.getDonorId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(DonorError.DocumentAlreadyExists.class)
  public ResponseEntity<?> handleDonorDocumentAlreadyExists(
      DonorError.DocumentAlreadyExists exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error", "donor_document_occupied",
                "message", "Donor document is already in use",
                "document", exception.getDocument()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(DonorError.InactiveDonor.class)
  public ResponseEntity<?> handleInactiveDonor(DonorError.InactiveDonor exception) {
    return ResponseEntity.unprocessableEntity()
        .body(Map.of("error", "inactive_donor", "message", exception.getMessage()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(DonationError.NotFound.class)
  public ResponseEntity<?> handleDonationNotFound(DonationError.NotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "donation_not_found",
                "message", "Donation not found",
                "donation_id", exception.getDonationId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(DonationError.InactiveDonation.class)
  public ResponseEntity<?> handleInactiveDonation(DonationError.InactiveDonation exception) {
    return ResponseEntity.unprocessableEntity()
        .body(Map.of("error", "inactive_donation", "message", exception.getMessage()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(DonationError.EmptyInputItems.class)
  public ResponseEntity<?> handleEmptyInputItems(DonationError.EmptyInputItems exception) {
    return ResponseEntity.badRequest()
        .body(Map.of("error", "empty_input_items", "message", exception.getMessage()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(DonationError.AttachmentNotFound.class)
  public ResponseEntity<?> handleAttachmentNotFound(DonationError.AttachmentNotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error",
                "attachment_not_found",
                "message",
                "Proof attachment not found",
                "attachment_id",
                exception.getAttachmentId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(MaterialError.NotFound.class)
  public ResponseEntity<?> handleMaterialNotFound(MaterialError.NotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "material_not_found",
                "message", exception.getMessage(),
                "material_id", exception.getMaterialId().toString(),
                "level", exception.getLevel()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(MaterialError.ParentNotFound.class)
  public ResponseEntity<?> handleMaterialParentNotFound(MaterialError.ParentNotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "material_parent_not_found",
                "message", exception.getMessage(),
                "parent_id", exception.getParentId().toString(),
                "parent_level", exception.getParentLevel()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(MaterialError.NameAlreadyExists.class)
  public ResponseEntity<?> handleMaterialNameConflict(MaterialError.NameAlreadyExists exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error", "material_name_occupied",
                "message", exception.getMessage(),
                "name", exception.getName(),
                "level", exception.getLevel()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(MaterialError.HasInventoryBinding.class)
  public ResponseEntity<?> handleMaterialInventoryBinding(
      MaterialError.HasInventoryBinding exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error", "material_has_inventory_binding",
                "message", exception.getMessage(),
                "material_id", exception.getMaterialId().toString(),
                "level", exception.getLevel()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(MaterialError.InactiveMaterial.class)
  public ResponseEntity<?> handleInactiveMaterial(MaterialError.InactiveMaterial exception) {
    return ResponseEntity.unprocessableEntity()
        .body(
            Map.of(
                "error", "inactive_material",
                "message", exception.getMessage(),
                "material_id", exception.getMaterialId().toString(),
                "level", exception.getLevel()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(MaterialError.ConcurrentModification.class)
  public ResponseEntity<?> handleConcurrentModification(
      MaterialError.ConcurrentModification exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error", "concurrent_modification",
                "message", exception.getMessage(),
                "material_id", exception.getMaterialId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(SortingError.NotFound.class)
  public ResponseEntity<?> handleSortingNotFound(SortingError.NotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "sorting_not_found",
                "message", exception.getMessage(),
                "sorting_id", exception.getSortingId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(SortingError.InputItemNotFound.class)
  public ResponseEntity<?> handleInputItemNotFound(SortingError.InputItemNotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error",
                "input_item_not_found",
                "message",
                exception.getMessage(),
                "input_item_id",
                exception.getInputItemId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(PressingError.NotFound.class)
  public ResponseEntity<?> handlePressingNotFound(PressingError.NotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error", "pressing_not_found",
                "message", exception.getMessage(),
                "pressing_id", exception.getPressingId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(PressingError.SortedItemNotFound.class)
  public ResponseEntity<?> handleSortedItemNotFound(PressingError.SortedItemNotFound exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            Map.of(
                "error",
                "sorted_item_not_found",
                "message",
                exception.getMessage(),
                "sorted_item_id",
                exception.getSortedItemId().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(PressingError.InvalidCompaction.class)
  public ResponseEntity<?> handleInvalidCompaction(PressingError.InvalidCompaction exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            Map.of(
                "error",
                "invalid_compaction",
                "message",
                exception.getMessage(),
                "initial_volume_m3",
                exception.getInitialVolumeM3().toString(),
                "final_volume_m3",
                exception.getFinalVolumeM3().toString()));
  }

  /**
   * @param exception
   * @return
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<?> handleOptimisticLock(OptimisticLockingFailureException exception) {
    log.warn("Optimistic lock conflict: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            Map.of(
                "error",
                "concurrent_modification",
                "message",
                "The record was modified by another transaction. Please retry."));
  }
}
