package org.code.api.controllers;

import java.io.IOException;
import java.util.Map;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Returns English boundary errors without exposing tokens, database details or storage paths.
 *
 * @author Enzo Ribas <a href="https://github.com/oEnzoRibas">@oEnzoRibas</a>
 */
@Order(-1)
@RestControllerAdvice(assignableTypes = {DocumentController.class, UserController.class})
public class BoundaryErrorHandler {
    /**
     * @param error rejected use case
     * @return documented status and safe reason
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> rejected(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("error", "request_rejected", "message", error.getReason() == null ? "Request rejected" : error.getReason()));
    }
    /**
     * @param error relational conflict
     * @return conflict without internal constraint details
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException error) {
        return ResponseEntity.status(409).body(Map.of("error", "record_conflict", "message", "Attachment is referenced by an operational record"));
    }
    /**
     * @param error storage failure
     * @return retryable service failure
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> unavailable(IOException error) {
        return ResponseEntity.status(503).body(Map.of("error", "storage_unavailable", "message", "Attachment storage is temporarily unavailable"));
    }
    /**
     * @param error oversized upload
     * @return explicit payload limit
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> tooLarge(MaxUploadSizeExceededException error) {
        return ResponseEntity.status(413).body(Map.of("error", "attachment_too_large", "message", "Multipart requests must not exceed 10 MiB"));
    }
}
