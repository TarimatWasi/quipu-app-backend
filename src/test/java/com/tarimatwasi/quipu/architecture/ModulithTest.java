package com.tarimatwasi.quipu.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tarimatwasi.quipu.QuipuApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** BE-SPR-ARQ-04, ARQ-05, ARQ-06, ARQ-07: module boundaries, APIs and cycles. */
class ModulithTest {

  @Test
  void modules_verify_passes() {
    var modules = ApplicationModules.of(QuipuApplication.class);

    modules.verify();

    // verify() is vacuous until the first business module (TAR-62): only the shared module
    // exists today. This assertion fails when a module is added, as a reminder to review that
    // the verification and its expectations cover it.
    assertThat(modules.stream().map(m -> m.getIdentifier().toString())).containsExactly("shared");
  }
}
