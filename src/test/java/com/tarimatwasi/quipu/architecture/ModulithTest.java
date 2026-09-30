package com.tarimatwasi.quipu.architecture;

import com.tarimatwasi.quipu.QuipuApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** BE-SPR-ARQ-04, ARQ-05, ARQ-06, ARQ-07: module boundaries, APIs and cycles. */
class ModulithTest {

  @Test
  void modules_verify_passes() {
    ApplicationModules.of(QuipuApplication.class).verify();
  }
}
