package com.tarimatwasi.quipu.auth.port.out;

import com.tarimatwasi.quipu.auth.domain.DocumentType;
import com.tarimatwasi.quipu.auth.domain.UserAccount;
import java.util.Optional;

public interface UserRepositoryPort {
  Optional<UserAccount> findByDocument(DocumentType documentType, String documentNumber);
}
