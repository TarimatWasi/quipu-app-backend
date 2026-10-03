package com.tarimatwasi.quipu.auth.application;

import com.tarimatwasi.quipu.auth.port.in.WeakPasswordException;
import java.nio.charset.StandardCharsets;

/** SEG-03 and the bcrypt limit: at least 8 characters, at most 72 bytes in UTF-8. */
final class PasswordPolicy {

  private static final int MIN_LENGTH = 8;
  private static final int MAX_BYTES = 72;

  private PasswordPolicy() {}

  static void require(String password) {
    if (password.codePointCount(0, password.length()) < MIN_LENGTH) {
      throw new WeakPasswordException(WeakPasswordException.Reason.TOO_SHORT);
    }
    if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
      throw new WeakPasswordException(WeakPasswordException.Reason.TOO_LONG);
    }
  }
}
