package com.tarimatwasi.quipu.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class JpaAuditingConfigTest {

  private static final String USER_ID = "3f2b8c1e-5d4a-4e6b-9c7d-1a2b3c4d5e6f";

  private final AuditorAware<String> auditor = new JpaAuditingConfig().auditorAware();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void auditsTheIdOfTheAuthenticatedUser() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));

    assertThat(auditor.getCurrentAuditor()).contains(USER_ID);
  }

  @Test
  void auditsSystemWhenThereIsNoSession() {
    assertThat(auditor.getCurrentAuditor()).contains("system");
  }

  @Test
  void auditsSystemForAnAnonymousRequest() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    assertThat(auditor.getCurrentAuditor()).contains("system");
  }
}
