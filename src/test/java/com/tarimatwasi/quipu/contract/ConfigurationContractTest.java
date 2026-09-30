package com.tarimatwasi.quipu.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.tarimatwasi.quipu.support.PostgresContainers;
import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.core.env.Environment;

/** BE-SPR-CFG-05, DAT-03, SEC-08, WEB-05 and QP-SPRMONO-TIM-01: effective configuration. */
@SpringBootTest
@ImportTestcontainers(PostgresContainers.class)
class ConfigurationContractTest {

  @Autowired Environment env;
  @Autowired Clock clock;

  @Test
  void jpa_validates_schema_and_closes_open_in_view() {
    assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    assertThat(env.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
  }

  @Test
  void local_is_the_default_profile() {
    // Perfil técnico de Quipu: without SPRING_PROFILES_ACTIVE the app runs as local; prod must be
    // activated explicitly and then fails without its variables (ProfilesContractTest).
    assertThat(env.getProperty("spring.profiles.default")).isEqualTo("local");
  }

  @Test
  void actuator_exposes_only_health() {
    assertThat(env.getProperty("management.endpoints.web.exposure.include")).isEqualTo("health");
  }

  @Test
  void error_responses_hide_internal_details() {
    for (var key : new String[] {"message", "stacktrace", "binding-errors", "exception"}) {
      assertThat(env.getProperty("server.error.include-" + key)).as(key).isEqualTo("never");
    }
  }

  @Test
  void clock_uses_lima_zone() {
    assertThat(clock.getZone()).isEqualTo(ZoneId.of("America/Lima"));
  }

  @Test
  void local_enablesTheApiDocs() {
    assertThat(env.getProperty("springdoc.api-docs.enabled")).isEqualTo("true");
    assertThat(env.getProperty("springdoc.api-docs.path")).isEqualTo("/api-docs");
    assertThat(env.getProperty("springdoc.swagger-ui.path")).isEqualTo("/swagger-ui.html");
  }
}
