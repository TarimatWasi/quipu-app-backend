package com.tarimatwasi.quipu.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * The three Spring profiles (Perfil técnico de Quipu): local is the default and the only one with
 * defaults; dev and prod are strict and differ in the API documentation.
 */
class ProfileFilesContractTest {

  /** A placeholder with a default, such as {@code ${PORT:8080}}. */
  private static final Pattern PLACEHOLDER_WITH_DEFAULT = Pattern.compile("\\$\\{[^}]*:[^}]*}");

  /**
   * The only optional variables of the strict profiles: once the first ADMIN exists they are no
   * longer needed (AdminBootstrap), so an empty default is legitimate.
   */
  private static final Pattern OPTIONAL_ADMIN_VARIABLE =
      Pattern.compile("\\$\\{ADMIN_(DOCUMENT_NUMBER|EMAIL|INITIAL_PASSWORD):}");

  private static boolean hasDefault(String value) {
    return PLACEHOLDER_WITH_DEFAULT
        .matcher(OPTIONAL_ADMIN_VARIABLE.matcher(value).replaceAll(""))
        .find();
  }

  private static Map<String, Object> load(String file) throws IOException {
    var sources = new YamlPropertySourceLoader().load(file, new ClassPathResource(file));
    var all = new java.util.HashMap<String, Object>();
    for (var source : sources) {
      var enumerable = (EnumerablePropertySource<?>) source;
      for (var name : enumerable.getPropertyNames()) {
        all.put(name, enumerable.getProperty(name));
      }
    }
    return all;
  }

  @Test
  void base_hasNoDefaultsAndApiDocsOff() throws IOException {
    var base = load("application.yml");

    assertThat(base.get("spring.profiles.default")).isEqualTo("local");
    assertThat(base.get("springdoc.api-docs.enabled")).isEqualTo(false);
    assertThat(base.get("springdoc.swagger-ui.enabled")).isEqualTo(false);
    assertThat(base.values().stream().map(String::valueOf))
        .noneMatch(ProfileFilesContractTest::hasDefault);
  }

  @ParameterizedTest
  @ValueSource(strings = {"application-dev.yml", "application-prod.yml"})
  void strictProfiles_haveNoDefaultsAndRequireTheAdmin(String file) throws IOException {
    var profile = load(file);

    assertThat(profile.values().stream().map(String::valueOf))
        .noneMatch(ProfileFilesContractTest::hasDefault);
    assertThat(profile.get("app.admin.required")).isEqualTo(true);
    assertThat(profile)
        .containsKeys("server.port", "spring.datasource.url", "app.cors.allowed-origin");
  }

  @ParameterizedTest
  @ValueSource(strings = {"application.yml", "application-dev.yml", "application-prod.yml"})
  void noProfileOverridesTheErrorDetailsToExposed(String file) throws IOException {
    assertThat(load(file).entrySet())
        .filteredOn(e -> e.getKey().startsWith("server.error.include-"))
        .allMatch(e -> "never".equals(e.getValue()));
  }

  @Test
  void apiDocs_onInLocalAndDev_offInProd() throws IOException {
    assertThat(load("application-local.yml").get("springdoc.api-docs.enabled")).isEqualTo(true);
    assertThat(load("application-dev.yml").get("springdoc.api-docs.enabled")).isEqualTo(true);
    assertThat(load("application-prod.yml").get("springdoc.api-docs.enabled")).isEqualTo(false);
    assertThat(load("application-prod.yml").get("springdoc.swagger-ui.enabled")).isEqualTo(false);
  }
}
