package com.tarimatwasi.quipu.auth.port.in;

import com.tarimatwasi.quipu.auth.domain.DocumentType;
import com.tarimatwasi.quipu.auth.domain.Role;

/** Authenticates a user by document and password. */
public interface LoginUseCase {

  /**
   * Checks the credentials.
   *
   * @throws com.tarimatwasi.quipu.auth.application.InvalidCredentialsException unknown document or
   *     <p>wrong password (the same error, to avoid revealing which)
   * @throws com.tarimatwasi.quipu.auth.application.AccountDisabledException the account is inactive
   */
  LoginResult login(LoginCommand command);

  /** Credentials presented at login. */
  record LoginCommand(DocumentType documentType, String documentNumber, String rawPassword) {}

  /** Outcome of a successful login. */
  record LoginResult(String userId, Role role, String displayEmail, boolean mustChangePassword) {}
}
