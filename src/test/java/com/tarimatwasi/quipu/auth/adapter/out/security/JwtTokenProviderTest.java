package com.tarimatwasi.quipu.auth.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private static final String SECRET = "test-secret-test-secret-test-secret-test-secret";

  private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60, Clock.systemUTC());

  @Test
  void parsesTokenItIssued() {
    String token = provider.issue("user-1", "ADMIN");

    assertThat(provider.parse(token))
        .hasValueSatisfying(
            s -> {
              assertThat(s.userId()).isEqualTo("user-1");
              assertThat(s.role()).isEqualTo("ADMIN");
            });
  }

  @Test
  void rejectsTokenSignedWithAnotherKey() {
    String forged =
        new JwtTokenProvider("other-secret-other-secret-other-secret-other", 60, Clock.systemUTC())
            .issue("user-1", "ADMIN");

    assertThat(provider.parse(forged)).isEmpty();
  }

  @Test
  void rejectsExpiredToken() {
    String expired = new JwtTokenProvider(SECRET, -1, Clock.systemUTC()).issue("user-1", "ADMIN");

    assertThat(provider.parse(expired)).isEmpty();
  }

  @Test
  @SuppressWarnings("NullAway") // intentional null: a signed token without that claim
  void rejectsSignedTokenWithoutRole() {
    assertThat(provider.parse(provider.issue("user-1", null))).isEmpty();
  }

  @Test
  @SuppressWarnings("NullAway") // intentional null: a signed token without that claim
  void rejectsSignedTokenWithoutSubject() {
    assertThat(provider.parse(provider.issue(null, "ADMIN"))).isEmpty();
  }

  @Test
  void rejectsGarbage() {
    assertThat(provider.parse("not-a-jwt")).isEmpty();
  }

  @Test
  void carriesThePendingPasswordChangeOfTheSession() {
    String token = provider.issue("user-1", "GUEST", true);

    assertThat(provider.parse(token))
        .hasValueSatisfying(s -> assertThat(s.mustChangePassword()).isTrue());
  }

  @Test
  void aTokenWithoutTheMarkHasNoPendingPasswordChange() {
    String token = provider.issue("user-1", "GUEST");

    assertThat(provider.parse(token))
        .hasValueSatisfying(s -> assertThat(s.mustChangePassword()).isFalse());
  }
}
