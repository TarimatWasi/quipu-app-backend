package com.tarimatwasi.quipu.bff.adapter.in.rest;

import com.tarimatwasi.quipu.auth.application.AccountDisabledException;
import com.tarimatwasi.quipu.auth.application.InvalidCredentialsException;
import com.tarimatwasi.quipu.auth.port.in.PasswordUnchangedException;
import com.tarimatwasi.quipu.auth.port.in.WeakPasswordException;
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

  /** RNF-08: the message for a short password is the one SRS 17 gives for the field. */
  @ExceptionHandler(WeakPasswordException.class)
  public ResponseEntity<BffErrorResponse> handleWeakPassword(WeakPasswordException e) {
    String message =
        e.reason() == WeakPasswordException.Reason.TOO_SHORT
            ? "Mínimo 8 caracteres"
            : "La contraseña es demasiado larga";
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new BffErrorResponse("AUTH_WEAK_PASSWORD", message, "newPassword"));
  }

  @ExceptionHandler(PasswordUnchangedException.class)
  public ResponseEntity<BffErrorResponse> handlePasswordUnchanged() {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            new BffErrorResponse(
                "AUTH_PASSWORD_UNCHANGED",
                "Elige una contraseña distinta de la temporal",
                "newPassword"));
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
