package com.tarimatwasi.quipu.auth.application;

public class InvalidCredentialsException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvalidCredentialsException() {
    super("Invalid document or password");
  }
}
