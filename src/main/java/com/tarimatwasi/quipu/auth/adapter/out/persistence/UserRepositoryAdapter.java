package com.tarimatwasi.quipu.auth.adapter.out.persistence;

import com.tarimatwasi.quipu.auth.domain.DocumentType;
import com.tarimatwasi.quipu.auth.domain.UserAccount;
import com.tarimatwasi.quipu.auth.port.out.UserRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

  private final UserJpaRepository jpaRepository;

  public UserRepositoryAdapter(UserJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<UserAccount> findByDocument(DocumentType documentType, String documentNumber) {
    return jpaRepository
        .findByDocumentTypeAndDocumentNumber(documentType, documentNumber)
        .map(UserJpaEntity::toDomain);
  }

  @Override
  public Optional<UserAccount> findById(UUID id) {
    return jpaRepository.findById(id).map(UserJpaEntity::toDomain);
  }

  @Override
  public void changePassword(UUID id, String newPasswordHash) {
    UserJpaEntity user =
        jpaRepository
            .findById(id)
            .orElseThrow(() -> new IllegalStateException("No user with id " + id));
    user.changePassword(newPasswordHash);
    jpaRepository.save(user);
  }
}
