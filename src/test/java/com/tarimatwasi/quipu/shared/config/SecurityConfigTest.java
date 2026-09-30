package com.tarimatwasi.quipu.shared.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** BE-SPR-SEC-05: the CORS origin is validated at startup. */
class SecurityConfigTest {

  @ParameterizedTest
  @ValueSource(strings = {"http://localhost:4200", "https://app.example.com"})
  void validateOrigin_exactOrigin_accepted(String origin) {
    assertThatCode(() -> SecurityConfig.validateOrigin(origin)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "*",
        "https://*.example.com",
        "https://app.example.com/",
        "https://app.example.com/path",
        "app.example.com",
        "ftp://app.example.com",
        ""
      })
  void validateOrigin_wildcardPathOrOtherScheme_rejected(String origin) {
    assertThatThrownBy(() -> SecurityConfig.validateOrigin(origin))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void validateOrigin_trailingSlash_messageNamesTheProperty() {
    assertThatThrownBy(() -> SecurityConfig.validateOrigin("https://a.example.com/"))
        .hasMessageContaining("app.cors.allowed-origin");
  }
}
