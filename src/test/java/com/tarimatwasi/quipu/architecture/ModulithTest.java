package com.tarimatwasi.quipu.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tarimatwasi.quipu.QuipuApplication;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** BE-SPR-ARQ-04, ARQ-05, ARQ-06, ARQ-07: module boundaries, APIs and cycles. */
class ModulithTest {

  /**
   * TAR-62 PR2: the imported code still has these violations by design. Remove this list (and the
   * assertion that it is not empty) when PR2 moves AuditableEntity, SecurityConfig and the bff to
   * auth access through port.in and the diagnostics services are exposed or moved.
   */
  private static final List<String> KNOWN_UNTIL_PR2 =
      List.of(
          "Cycle detected: Slice auth",
          "Module 'auth' depends on non-exposed type com.tarimatwasi.quipu.shared.domain.",
          "Module 'bff' depends on non-exposed type com.tarimatwasi.quipu.auth.",
          "Module 'bff' depends on non-exposed type com.tarimatwasi.quipu.shared.adapter.",
          "Module 'shared' depends on non-exposed type com.tarimatwasi.quipu.auth.");

  @Test
  void modules_have_no_violations_beyond_the_known_ones() {
    var modules = ApplicationModules.of(QuipuApplication.class);

    var messages = modules.detectViolations().getMessages();
    var unexpected =
        messages.stream().filter(m -> KNOWN_UNTIL_PR2.stream().noneMatch(m::contains)).toList();

    assertThat(unexpected).as("violations not in the TAR-62 PR2 list").isEmpty();
    // Fails once PR2 fixes them, as a reminder to delete KNOWN_UNTIL_PR2.
    assertThat(messages).as("TAR-62 PR2 list is still needed").isNotEmpty();
    assertThat(modules.stream().map(m -> m.getIdentifier().toString()))
        .containsExactlyInAnyOrder("auth", "bff", "shared");
  }
}
