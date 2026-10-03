package com.tarimatwasi.quipu.contract;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRules;
import com.tarimatwasi.quipu.support.PostgresContainers;
import jakarta.servlet.http.Cookie;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * TAR-23: the real answers of the BFF must satisfy the shared contract. The spec is the one of the
 * tag pinned in pom.xml (contract.version, contract.sha256), downloaded and hash-checked by the
 * build into target/contract/bff.yaml. Only the operations the backend already implements are
 * exercised; the rest of the spec is covered as they are built.
 */
@SpringBootTest(properties = "app.cors.allowed-origin=http://localhost:3000")
@AutoConfigureMockMvc
@ImportTestcontainers(PostgresContainers.class)
class BffContractTest {

  private static final Path SPEC_FILE = Path.of("target/contract/bff.yaml").toAbsolutePath();
  private static final String SPEC = SPEC_FILE.toString();
  private static final String LOGIN = "/bff/auth/login";
  private static final String CHANGE_PASSWORD = "/bff/auth/change-password";
  private static final String TEMPORARY = "Temporal123!";

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder passwordEncoder;

  /** The PostgreSQL container is shared by all integration tests: this test owns its users. */
  @BeforeEach
  void oneAdminWithATemporaryPassword() {
    assertThat(SPEC_FILE)
        .as("the build downloads the contract (pom.xml, contract.version)")
        .exists();
    jdbc.update("DELETE FROM users");
    insertUser("00000000", "ADMIN", "ACTIVE", true);
  }

  /** The request and the response must both satisfy the contract. */
  private static ResultMatcher satisfiesTheContract() {
    return openApi().isValid(SPEC);
  }

  /**
   * For a request that is invalid on purpose: the response is validated in full, and only the one
   * request-side complaint named by {@code requestErrorKey} is ignored.
   */
  private static ResultMatcher answersTheContractToARequestWith(String requestErrorKey) {
    var validator =
        OpenApiInteractionValidator.createFor(SPEC)
            .withWhitelist(
                ValidationErrorsWhitelist.create()
                    .withRule(
                        "the request is invalid on purpose",
                        WhitelistRules.allOf(
                            WhitelistRules.isRequest(),
                            WhitelistRules.messageHasKey(requestErrorKey))))
            .build();
    return openApi().isValid(validator);
  }

  private void insertUser(String document, String role, String status, boolean mustChange) {
    jdbc.update(
        "INSERT INTO users (id, email, document_type, document_number, password_hash, role,"
            + " must_change_password, status) VALUES (?, ?, 'DNI', ?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        document + "@example.test",
        document,
        passwordEncoder.encode(TEMPORARY),
        role,
        mustChange,
        status);
  }

  private ResultActions login(String document, String password) throws Exception {
    var body =
        "{\"documentType\":\"DNI\",\"documentNumber\":\"%s\",\"password\":\"%s\"}"
            .formatted(document, password);
    return mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private Cookie sessionOf(ResultActions login) {
    return Objects.requireNonNull(login.andReturn().getResponse().getCookie("sessionToken"));
  }

  private ResultActions changePassword(Cookie session, String body) throws Exception {
    return mockMvc.perform(
        post(CHANGE_PASSWORD)
            .cookie(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void loginOk() throws Exception {
    login("00000000", TEMPORARY).andExpect(status().isOk()).andExpect(satisfiesTheContract());
  }

  @Test
  void loginWithWrongCredentials() throws Exception {
    login("00000000", "wrong-password")
        .andExpect(status().isUnauthorized())
        .andExpect(satisfiesTheContract());
  }

  @Test
  void loginOfADisabledAccount() throws Exception {
    insertUser("11111111", "GUEST", "INACTIVE", false);

    login("11111111", TEMPORARY)
        .andExpect(status().isForbidden())
        .andExpect(satisfiesTheContract());
  }

  @Test
  void changePasswordOk() throws Exception {
    Cookie session = sessionOf(login("00000000", TEMPORARY));

    changePassword(session, "{\"newPassword\":\"Nueva12345\"}")
        .andExpect(status().isNoContent())
        .andExpect(satisfiesTheContract());
  }

  @Test
  void changePasswordWithAWeakPassword() throws Exception {
    Cookie session = sessionOf(login("00000000", TEMPORARY));

    changePassword(session, "{\"newPassword\":\"corta\"}")
        .andExpect(status().isBadRequest())
        .andExpect(answersTheContractToARequestWith("validation.request.body.schema.minLength"));
  }

  @Test
  void changePasswordKeepingTheTemporaryOne() throws Exception {
    Cookie session = sessionOf(login("00000000", TEMPORARY));

    changePassword(session, "{\"newPassword\":\"" + TEMPORARY + "\"}")
        .andExpect(status().isBadRequest())
        .andExpect(satisfiesTheContract());
  }

  @Test
  void changePasswordWithoutASession() throws Exception {
    mockMvc
        .perform(
            post(CHANGE_PASSWORD)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"Nueva12345\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_NO_SESSION"))
        .andExpect(answersTheContractToARequestWith("validation.request.security.missing"));
  }
}
