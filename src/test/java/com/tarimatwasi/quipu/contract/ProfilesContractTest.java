package com.tarimatwasi.quipu.contract;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tarimatwasi.quipu.QuipuApplication;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * BE-SPR-CFG-05: dev and prod have no defaults, so activating them without the required variables
 * fails the startup instead of running with development values.
 */
class ProfilesContractTest {

  @ParameterizedTest
  @ValueSource(strings = {"dev", "prod"})
  void strictProfile_withoutRequiredVariables_failsToStart(String profile) {
    var builder =
        new SpringApplicationBuilder(QuipuApplication.class)
            .web(WebApplicationType.NONE)
            .profiles(profile);

    // Boot leaves an unresolved ${VAR} as literal text while binding, so the failure comes from
    // the first consumer that validates it (for example the JDBC url), not from the placeholder.
    assertThatThrownBy(() -> builder.run().close()).isInstanceOf(RuntimeException.class);
  }
}
