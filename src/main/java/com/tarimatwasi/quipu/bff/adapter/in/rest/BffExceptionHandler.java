package com.tarimatwasi.quipu.bff.adapter.in.rest;

import com.tarimatwasi.quipu.auth.application.AccountDisabledException;
import com.tarimatwasi.quipu.auth.application.InvalidCredentialsException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Before Boot's ProblemDetails advice: the BFF answers {code, message, field?}, not problem+json.
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.tarimatwasi.quipu.bff")
public class BffExceptionHandler {

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<BffErrorResponse> handleInvalidCredentials() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            new BffErrorResponse("AUTH_INVALID_CREDENTIALS", "Documento o contraseña incorrectos"));
  }

  @ExceptionHandler(AccountDisabledException.class)
  public ResponseEntity<BffErrorResponse> handleAccountDisabled() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new BffErrorResponse("AUTH_ACCOUNT_DISABLED", "Cuenta deshabilitada"));
  }

  /** QP-SPRMONO-BFF-01: stable English code, Spanish message, and the first invalid field. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BffErrorResponse> handleInvalidInput(MethodArgumentNotValidException e) {
    String field =
        e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField())
            .sorted()
            .findFirst()
            .orElse(null);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new BffErrorResponse("VALIDATION_ERROR", "Datos de entrada inválidos", field));
  }
}
