package com.tarimatwasi.quipu.auth.domain;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record UserAccount(
    UUID id,
    String email,
    DocumentType documentType,
    String documentNumber,
    String passwordHash,
    Role role,
    @Nullable UUID guestId,
    boolean mustChangePassword,
    String status) {
  public boolean isDisabled() {
    return "INACTIVE".equals(status);
  }
}
