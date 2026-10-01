package com.tarimatwasi.quipu.bff.adapter.in.rest;

import com.tarimatwasi.quipu.auth.application.AccountDisabledException;
import com.tarimatwasi.quipu.auth.application.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
