package com.tarimatwasi.quipu.auth.port.out;

import com.tarimatwasi.quipu.auth.domain.DocumentType;
import com.tarimatwasi.quipu.auth.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {
  Optional<UserAccount> findByDocument(DocumentType documentType, String documentNumber);

  Optional<UserAccount> findById(UUID id);

  /** Stores the new hash and clears the pending-change flag of the account. */
  void changePassword(UUID id, String newPasswordHash);
}
