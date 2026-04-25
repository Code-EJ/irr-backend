package org.code.api.infrastructure.exceptions;

        import org.code.api.domain.exception.AuthError;
        import org.code.api.domain.exception.VehicleError;
        import org.springframework.dao.DataIntegrityViolationException;
        import org.springframework.http.HttpStatus;
        import org.springframework.http.ResponseEntity;
        import org.springframework.http.converter.HttpMessageNotReadableException;
        import org.springframework.security.access.AccessDeniedException;
        import org.springframework.validation.FieldError;
        import org.springframework.web.bind.MethodArgumentNotValidException;
        import org.springframework.web.bind.annotation.ExceptionHandler;
        import org.springframework.web.bind.annotation.RestControllerAdvice;
        import org.springframework.web.servlet.NoHandlerFoundException;

        import lombok.extern.slf4j.Slf4j;

        import java.time.Instant;
        import java.util.LinkedHashMap;
        import java.util.Map;

        /**
         * Global and centralized interceptor to standardize all API error responses.
         *
         * <p>This class maps domain and framework exceptions to consistent HTTP responses
         * containing a timestamp, status code, machine-friendly error code, human message,
         * and optional extra fields with contextual information.
         */
        @Slf4j
        @RestControllerAdvice
        public class GlobalExceptionHandler {

            // ==========================================
            // 1. VALIDATION & HTTP PAYLOAD ERRORS (400)
            // ==========================================

            /**
             * Handles Spring validation errors (Bean Validation).
             *
             * @param ex the MethodArgumentNotValidException thrown by the framework
             * @return a standardized 400 Bad Request response with the first validation message
             */
            @ExceptionHandler(MethodArgumentNotValidException.class)
            public ResponseEntity<Object> handleValidations(MethodArgumentNotValidException ex) {
                FieldError firstError = ex.getBindingResult().getFieldErrors().get(0);
                return buildResponse(HttpStatus.BAD_REQUEST, "validation_failed", firstError.getDefaultMessage());
            }

            /**
             * Handles JSON parse errors and malformed request bodies.
             *
             * @param ex the HttpMessageNotReadableException thrown when the request body is invalid
             * @return a standardized 400 Bad Request response describing the issue
             */
            @ExceptionHandler(HttpMessageNotReadableException.class)
            public ResponseEntity<Object> handleJsonParseError(HttpMessageNotReadableException ex) {
                return buildResponse(HttpStatus.BAD_REQUEST, "malformed_json", "The request body is invalid or missing.");
            }

            // ==========================================
            // 2. DOMAIN ERRORS: AUTHENTICATION & SESSION
            // ==========================================

            /**
             * Handles invalid creator user identification errors.
             *
             * @param ex the CreatorUserInvalid domain exception
             * @return 400 Bad Request with details about the invalid creator id
             */
            @ExceptionHandler(AuthError.CreatorUserInvalid.class)
            public ResponseEntity<Object> handleCreatorUserInvalid(AuthError.CreatorUserInvalid ex) {
                String errorCode = ex.isInvalidUUID() ? "invalid_creator_uuid" : "creator_user_not_found";
                String errorMessage = ex.isInvalidUUID()
                        ? "The provided creator user ID is not a valid UUID"
                        : "No user found with the provided creator user ID";

                log.debug("{} - {} - {}", errorCode, ex.getCreatorUserId(), errorMessage);
                return buildResponse(HttpStatus.BAD_REQUEST, errorCode, errorMessage, Map.of("creator_user_id", ex.getCreatorUserId()));
            }

            /**
             * Handles attempts to register an already used email.
             *
             * @param ex the EmailOccupied domain exception
             * @return 409 Conflict indicating the email is already in use
             */
            @ExceptionHandler(AuthError.EmailOccupied.class)
            public ResponseEntity<Object> handleEmailOccupied(AuthError.EmailOccupied ex) {
                log.debug("Attempt to register an email that is already occupied {}", ex.getEmail());
                return buildResponse(HttpStatus.CONFLICT, "email_occupied", "The email address is already in use: " + ex.getEmail(), Map.of("email", ex.getEmail()));
            }

            /**
             * Handles invalid token usage.
             *
             * @param ex the InvalidToken domain exception
             * @return 401 Unauthorized indicating the token is invalid
             */
            @ExceptionHandler(AuthError.InvalidToken.class)
            public ResponseEntity<Object> handleInvalidToken(AuthError.InvalidToken ex) {
                log.debug("Invalid token used: {}", ex.getToken());
                return buildResponse(HttpStatus.UNAUTHORIZED, "invalid_token", "The provided token is invalid.", Map.of("token", ex.getToken()));
            }

            /**
             * Handles expired token scenarios.
             *
             * @param ex the ExpiredToken domain exception
             * @return 403 Forbidden indicating the token has expired with timestamps included
             */
            @ExceptionHandler(AuthError.ExpiredToken.class)
            public ResponseEntity<Object> handleExpiredToken(AuthError.ExpiredToken ex) {
                log.debug("Expired token used, issued at: {}, expires at: {}", ex.getIssuedAt(), ex.getExpiresAt());
                return buildResponse(HttpStatus.FORBIDDEN, "expired_token", "The provided token has expired.",
                        Map.of("expiration_time", ex.getExpiresAt().getEpochSecond(), "issued_at", ex.getIssuedAt().getEpochSecond()));
            }

            /**
             * Handles wrong credentials (email not found or password mismatch).
             *
             * @param ex the WrongCredentials domain exception
             * @return 401 Unauthorized with the email that caused the failure
             */
            @ExceptionHandler(AuthError.WrongCredentials.class)
            public ResponseEntity<Object> handleWrongCredentials(AuthError.WrongCredentials ex) {
                log.debug(ex.isUserValid() ? "Password mismatch for email: " + ex.getEmail() : "No user found with email: " + ex.getEmail());
                return buildResponse(HttpStatus.UNAUTHORIZED, "wrong_credentials", "The provided email or password is incorrect.", Map.of("email", ex.getEmail()));
            }

            /**
             * Handles passwords that exceed allowed length.
             *
             * @param ex the PasswordTooLong domain exception
             * @return 400 Bad Request with the actual password length
             */
            @ExceptionHandler(AuthError.PasswordTooLong.class)
            public ResponseEntity<Object> handlePasswordTooLong(AuthError.PasswordTooLong ex) {
                log.debug("Attempt to register with a password exceeding 72 bytes: {} bytes", ex.getPasswordLength());
                return buildResponse(HttpStatus.BAD_REQUEST, "password_too_long", "The provided password exceeds the maximum allowed length of 72 bytes.", Map.of("password_length", ex.getPasswordLength()));
            }

            /**
             * Handles unauthorized access attempts that require authentication.
             *
             * @param ex the Unauthorized domain exception
             * @return 401 Unauthorized with the exception message
             */
            @ExceptionHandler(AuthError.Unauthorized.class)
            public ResponseEntity<Object> handleUnauthorized(AuthError.Unauthorized ex) {
                return buildResponse(HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage());
            }

            // ==========================================
            // 3. DOMAIN ERRORS: VEHICLE & FLEET
            // ==========================================

            /**
             * Handles vehicle not found errors.
             *
             * @param ex the VehicleError.NotFound exception
             * @return 404 Not Found with vehicle id context
             */
            @ExceptionHandler(VehicleError.NotFound.class)
            public ResponseEntity<Object> handleVehicleNotFound(VehicleError.NotFound ex) {
                return buildResponse(HttpStatus.NOT_FOUND, "vehicle_not_found", "Vehicle not found", Map.of("vehicle_id", ex.getVehicleId()));
            }

            /**
             * Handles vehicle plate uniqueness violations.
             *
             * @param ex the VehicleError.PlateAlreadyExists exception
             * @return 409 Conflict indicating the plate is already used
             */
            @ExceptionHandler(VehicleError.PlateAlreadyExists.class)
            public ResponseEntity<Object> handleVehiclePlateAlreadyExists(VehicleError.PlateAlreadyExists ex) {
                return buildResponse(HttpStatus.CONFLICT, "vehicle_plate_occupied", "Vehicle plate is already in use", Map.of("placa", ex.getPlaca()));
            }

            /**
             * Handles missing authenticated user in session for vehicle operations.
             *
             * @param ex the VehicleError.SessionUserNotFound exception
             * @return 401 Unauthorized with the missing user id
             */
            @ExceptionHandler(VehicleError.SessionUserNotFound.class)
            public ResponseEntity<Object> handleVehicleSessionUserNotFound(VehicleError.SessionUserNotFound ex) {
                return buildResponse(HttpStatus.UNAUTHORIZED, "invalid_session_user", "Authenticated user not found", Map.of("user_id", ex.getUserId()));
            }

            /**
             * Handles access denied for vehicle operations (e.g., only admins may deactivate).
             *
             * @param ex the VehicleError.AccessDenied exception
             * @return 403 Forbidden with the user type context
             */
            @ExceptionHandler(VehicleError.AccessDenied.class)
            public ResponseEntity<Object> handleVehicleAccessDenied(VehicleError.AccessDenied ex) {
                return buildResponse(HttpStatus.FORBIDDEN, "vehicle_access_denied", "Only administrators can deactivate vehicles", Map.of("user_type", ex.getUserType().name()));
            }

            // ==========================================
            // 4. GENERIC AND SECURITY ERRORS (SPRING)
            // ==========================================

            /**
             * Handles Spring Security access denied exceptions.
             *
             * @param ex the AccessDeniedException from Spring Security
             * @return 403 Forbidden with a standardized message
             */
            @ExceptionHandler(AccessDeniedException.class)
            public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
                return buildResponse(HttpStatus.FORBIDDEN, "forbidden", "You do not have permission to access this resource.");
            }

            /**
             * Handles requests to non-existing endpoints.
             *
             * @param ex the NoHandlerFoundException when no mapping matches the request
             * @return 404 Not Found with a standardized message
             */
            @ExceptionHandler(NoHandlerFoundException.class)
            public ResponseEntity<Object> handleNotFound(NoHandlerFoundException ex) {
                return buildResponse(HttpStatus.NOT_FOUND, "resource_not_found", "The requested endpoint does not exist.");
            }

            /**
             * Handles database integrity violations (e.g., unique constraint).
             *
             * @param ex the DataIntegrityViolationException from the persistence layer
             * @return 409 Conflict with a generic conflict message
             */
            @ExceptionHandler(DataIntegrityViolationException.class)
            public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex) {
                return buildResponse(HttpStatus.CONFLICT, "conflict", "A conflicting record already exists in the database for the provided data.");
            }

            /**
             * Catches any uncaught exceptions to avoid leaking internals and to log the error.
             *
             * @param ex the uncaught Exception
             * @return 500 Internal Server Error with a generic message
             */
            @ExceptionHandler(Exception.class)
            public ResponseEntity<Object> handleUncaughtException(Exception ex) {
                log.error("Unhandled internal error: ", ex);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred on the server.");
            }

            // ==========================================
            // HELPER METHODS (DRY)
            // ==========================================

            /**
             * Convenience overload without extra fields.
             */
            private ResponseEntity<Object> buildResponse(HttpStatus status, String error, String message) {
                return buildResponse(status, error, message, null);
            }

            /**
             * Builds a standardized error response body with optional extra fields.
             *
             * @param status      HTTP status to return
             * @param error       machine-friendly error code
             * @param message     human-friendly message
             * @param extraFields optional map with additional context fields
             * @return a ResponseEntity containing the constructed body and status
             */
            private ResponseEntity<Object> buildResponse(HttpStatus status, String error, String message, Map<String, Object> extraFields) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("timestamp", Instant.now().toString());
                body.put("status", status.value());
                body.put("error", error);
                body.put("message", message);

                if (extraFields != null && !extraFields.isEmpty()) {
                    body.putAll(extraFields);
                }

                return new ResponseEntity<>(body, status);
            }
        }