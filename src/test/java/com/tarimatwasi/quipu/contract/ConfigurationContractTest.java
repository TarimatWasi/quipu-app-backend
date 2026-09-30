package com.tarimatwasi.quipu.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.tarimatwasi.quipu.support.PostgresContainers;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

/** BE-SPR-CFG-05, DAT-03, SEC-08, WEB-05 and QP-SPRMONO-TIM-01: effective configuration. */
@SpringBootTest
@ImportTestcontainers(PostgresContainers.class)
class ConfigurationContractTest {

  /** A placeholder with a default, such as {@code ${PORT:8080}}. */
  private static final Pattern PLACEHOLDER_WITH_DEFAULT = Pattern.compile("\\$\\{[^}]*:[^}]*}");

  @Autowired Environment env;
  @Autowired Clock clock;

  @Test
  void jpa_validates_schema_and_closes_open_in_view() {
    assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    assertThat(env.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
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
  void base_and_prod_profiles_define_no_defaults() throws IOException {
    // prod inherits application.yml, so neither file may carry a default value.
    for (var file : new String[] {"application.yml", "application-prod.yml"}) {
      var content =
          new ClassPathResource(file).getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
      assertThat(PLACEHOLDER_WITH_DEFAULT.matcher(content).find())
          .as("%s must not contain ${VAR:default}", file)
          .isFalse();
    }
  }
}
