package com.tarimatwasi.quipu.auth.application;

public class AccountDisabledException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public AccountDisabledException() {
    super("Account is disabled");
  }
}
