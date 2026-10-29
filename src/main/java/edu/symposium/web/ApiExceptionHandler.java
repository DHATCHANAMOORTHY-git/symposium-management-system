package edu.symposium.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RegistrationController.class)
public class ApiExceptionHandler {
    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ApiError> handleRegistrationException(RegistrationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiError(false, exception.getMessage(), exception.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid registration data.");
        return ResponseEntity.badRequest()
                .body(new ApiError(false, message, "INVALID_REGISTRATION_DATA"));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new ApiError(false, "Invalid registration data.", "INVALID_REGISTRATION_DATA"));
    }

    public record ApiError(boolean success, String message, String code) {
    }
}
