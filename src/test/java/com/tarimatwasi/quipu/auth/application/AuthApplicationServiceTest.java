package com.tarimatwasi.quipu.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tarimatwasi.quipu.auth.domain.DocumentType;
import com.tarimatwasi.quipu.auth.domain.Role;
import com.tarimatwasi.quipu.auth.domain.UserAccount;
import com.tarimatwasi.quipu.auth.port.in.LoginUseCase.LoginCommand;
import com.tarimatwasi.quipu.auth.port.in.LoginUseCase.LoginResult;
import com.tarimatwasi.quipu.auth.port.out.UserRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthApplicationServiceTest {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private InMemoryUserRepository repository;
  private AuthApplicationService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryUserRepository();
    service = new AuthApplicationService(repository, encoder);
  }

  @Test
  void logsInWithCorrectDocumentAndPassword() {
    UserAccount admin =
        new UserAccount(
            UUID.randomUUID(),
            "admin@tarimatwasi.local",
            DocumentType.DNI,
            "00000000",
            encoder.encode("Temporal123!"),
            Role.ADMIN,
            null,
            true,
            "ACTIVE");
    repository.save(admin);

    LoginResult result =
        service.login(new LoginCommand(DocumentType.DNI, "00000000", "Temporal123!"));

    assertThat(result.role()).isEqualTo(Role.ADMIN);
    assertThat(result.mustChangePassword()).isTrue();
  }

  @Test
  void rejectsWrongPasswordWithGenericError() {
    UserAccount admin =
        new UserAccount(
            UUID.randomUUID(),
            "admin@tarimatwasi.local",
            DocumentType.DNI,
            "00000000",
            encoder.encode("Temporal123!"),
            Role.ADMIN,
            null,
            true,
            "ACTIVE");
    repository.save(admin);

    assertThatThrownBy(() -> service.login(new LoginCommand(DocumentType.DNI, "00000000", "wrong")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void rejectsUnknownDocumentWithSameGenericError() {
    assertThatThrownBy(
            () -> service.login(new LoginCommand(DocumentType.DNI, "99999999", "whatever")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  private static class InMemoryUserRepository implements UserRepositoryPort {
    private final java.util.Map<String, UserAccount> byDocument = new java.util.HashMap<>();

    void save(UserAccount user) {
      byDocument.put(user.documentType() + ":" + user.documentNumber(), user);
    }

    @Override
    public Optional<UserAccount> findByDocument(DocumentType documentType, String documentNumber) {
      return Optional.ofNullable(byDocument.get(documentType + ":" + documentNumber));
    }
  }
}
