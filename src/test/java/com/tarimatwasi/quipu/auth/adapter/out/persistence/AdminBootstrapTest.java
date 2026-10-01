package com.tarimatwasi.quipu.auth.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

  private static final ApplicationArguments NO_ARGS = new DefaultApplicationArguments();

  private static final String COUNT_ADMINS =
      "SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND status <> 'INACTIVE'";

  @Mock JdbcTemplate jdbc;
  @Mock PasswordEncoder encoder;

  @BeforeEach
  void noAdminYet() {
    when(jdbc.queryForObject(COUNT_ADMINS, Integer.class)).thenReturn(0);
  }

  @Test
  void doesNothingWhenAnAdminAlreadyExists() {
    when(jdbc.queryForObject(COUNT_ADMINS, Integer.class)).thenReturn(1);

    new AdminBootstrap(jdbc, encoder, "12345678", "a@b.c", "long-enough-pw", false).run(NO_ARGS);

    verify(jdbc, never()).update(anyString(), (Object[]) any());
  }

  @Test
  void doesNotFailWhenNothingIsConfigured() {
    assertThatCode(() -> new AdminBootstrap(jdbc, encoder, "", "", "", false).run(NO_ARGS))
        .doesNotThrowAnyException();

    verify(jdbc, never()).update(anyString(), (Object[]) any());
  }

  @Test
  void failsWhenAdminIsRequiredAndNothingIsConfigured() {
    assertThatThrownBy(() -> new AdminBootstrap(jdbc, encoder, "", "", "", true).run(NO_ARGS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("nobody could access");
  }

  @Test
  void doesNotFailWhenAdminIsRequiredButOneActiveAlreadyExists() {
    when(jdbc.queryForObject(COUNT_ADMINS, Integer.class)).thenReturn(1);

    assertThatCode(() -> new AdminBootstrap(jdbc, encoder, "", "", "", true).run(NO_ARGS))
        .doesNotThrowAnyException();
  }

  @Test
  void failsFastOnPartialConfiguration() {
    assertThatThrownBy(
            () ->
                new AdminBootstrap(jdbc, encoder, "12345678", "", "long-enough-pw", false)
                    .run(NO_ARGS))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void rejectsShortPassword() {
    assertThatThrownBy(
            () ->
                new AdminBootstrap(jdbc, encoder, "12345678", "a@b.c", "short", false).run(NO_ARGS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("8");
  }

  @Test
  void toleratesConcurrentCreationByAnotherInstance() {
    stubDuplicateInsert();
    when(jdbc.queryForObject(COUNT_ADMINS, Integer.class)).thenReturn(0, 1);

    assertThatCode(
            () ->
                new AdminBootstrap(jdbc, encoder, "12345678", "a@b.c", "long-enough-pw", false)
                    .run(NO_ARGS))
        .doesNotThrowAnyException();
  }

  @Test
  void failsWhenDuplicateKeyBelongsToNonAdminUser() {
    stubDuplicateInsert();

    assertThatThrownBy(
            () ->
                new AdminBootstrap(jdbc, encoder, "12345678", "a@b.c", "long-enough-pw", false)
                    .run(NO_ARGS))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not an active ADMIN");
  }

  private void stubDuplicateInsert() {
    when(encoder.encode("long-enough-pw")).thenReturn("hashed");
    when(jdbc.update(
            org.mockito.ArgumentMatchers.contains("INSERT"),
            any(java.util.UUID.class),
            org.mockito.ArgumentMatchers.eq("a@b.c"),
            org.mockito.ArgumentMatchers.eq("12345678"),
            org.mockito.ArgumentMatchers.eq("hashed")))
        .thenThrow(new DuplicateKeyException("dup"));
  }

  @Test
  void createsAdminWithHashedPasswordAndForcedChange() {
    when(encoder.encode("long-enough-pw")).thenReturn("hashed");

    new AdminBootstrap(jdbc, encoder, "12345678", "a@b.c", "long-enough-pw", false).run(NO_ARGS);

    verify(jdbc)
        .update(
            org.mockito.ArgumentMatchers.contains("must_change_password"),
            any(java.util.UUID.class),
            org.mockito.ArgumentMatchers.eq("a@b.c"),
            org.mockito.ArgumentMatchers.eq("12345678"),
            org.mockito.ArgumentMatchers.eq("hashed"));
  }
}
