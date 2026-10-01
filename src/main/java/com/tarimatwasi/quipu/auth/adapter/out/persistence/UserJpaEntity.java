package com.tarimatwasi.quipu.auth.adapter.out.persistence;

import com.tarimatwasi.quipu.shared.domain.AuditableEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity extends AuditableEntity {

  @Id private UUID id;

  private String email;

  @Column(name = "document_type")
  @Enumerated(EnumType.STRING)
  private com.tarimatwasi.quipu.auth.domain.DocumentType documentType;

  @Column(name = "document_number")
  private String documentNumber;

  @Column(name = "password_hash")
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  private com.tarimatwasi.quipu.auth.domain.Role role;

  @Column(name = "guest_id")
  private UUID guestId;

  @Column(name = "must_change_password")
  private boolean mustChangePassword;

  private String status;

  protected UserJpaEntity() {}

  public com.tarimatwasi.quipu.auth.domain.UserAccount toDomain() {
    return new com.tarimatwasi.quipu.auth.domain.UserAccount(
        id,
        email,
        documentType,
        documentNumber,
        passwordHash,
        role,
        guestId,
        mustChangePassword,
        status);
  }
}
