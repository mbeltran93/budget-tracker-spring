package com.mbeltran.budgettracker.controller.advice;

import com.mbeltran.budgettracker.dto.ApiError;
import com.mbeltran.budgettracker.exception.DuplicateResourceException;
import com.mbeltran.budgettracker.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Manejo de errores para los controllers REST internos (/api/**): siempre JSON,
 * nunca una pagina Thymeleaf.
 */
@RestControllerAdvice(basePackages = "com.mbeltran.budgettracker.controller.rest")
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiError.of(HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), List.of()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiError.of(HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), List.of()));
    }

    /** Credenciales invalidas en POST /api/auth/login o /api/auth/oauth/google: 401, no 500. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Credenciales invalidas", List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request", "Datos invalidos", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                        "Ocurrio un error inesperado", List.of()));
    }
}
