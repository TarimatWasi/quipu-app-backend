package com.tarimatwasi.quipu.bff.adapter.in.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tarimatwasi.quipu.support.PostgresContainers;
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

/** Login flow of the BFF against a real database and the bootstrapped ADMIN. */
@SpringBootTest(properties = "app.cors.allowed-origin=http://localhost:3000")
@AutoConfigureMockMvc
@ImportTestcontainers(PostgresContainers.class)
class AuthBffControllerTest {

  private static final String LOGIN = "/bff/auth/login";

  private static final String ADMIN_LOGIN_BODY =
      """
      {"documentType":"DNI","documentNumber":"00000000","password":"Temporal123!"}
      """;

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder passwordEncoder;

  /** The PostgreSQL container is shared by all integration tests: this test owns its ADMIN. */
  @BeforeEach
  void adminExists() {
    jdbc.update("DELETE FROM users");
    jdbc.update(
        "INSERT INTO users (id, email, document_type, document_number, password_hash, role,"
            + " must_change_password, status)"
            + " VALUES (?, 'admin@example.test', 'DNI', '00000000', ?, 'ADMIN', TRUE, 'ACTIVE')",
        UUID.randomUUID(),
        passwordEncoder.encode("Temporal123!"));
  }

  @Test
  void loginWithAdminSucceedsAndSetsCookie() throws Exception {
    mockMvc
        .perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(ADMIN_LOGIN_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.mustChangePassword").value(true))
        .andExpect(cookie().exists("sessionToken"))
        .andExpect(cookie().httpOnly("sessionToken", true));
  }

  @Test
  void loginWithWrongPasswordReturns401GenericError() throws Exception {
    var body =
        """
        {"documentType":"DNI","documentNumber":"00000000","password":"wrong"}
        """;

    mockMvc
        .perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void loginWithUnknownDocumentReturnsSame401AsWrongPassword() throws Exception {
    var body =
        """
        {"documentType":"DNI","documentNumber":"99999999","password":"anything"}
        """;

    mockMvc
        .perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
  }

  @Test
  void loginWithFormUrlencodedContentTypeIsRejectedBeforeLoginLogic() throws Exception {
    mockMvc
        .perform(
            post(LOGIN)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("documentType=DNI&documentNumber=00000000&password=Temporal123!"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  void crossSiteBodilessPostIsStoppedByCors() throws Exception {
    // A bodiless POST carries no Content-Type, so JsonOnlyFilter lets it through; a cross-site one
    // always sends Origin and exact-origin CORS rejects it (ADR-006, reviewed in TAR-59).
    mockMvc
        .perform(post(LOGIN).header("Origin", "https://evil.example.com"))
        .andExpect(status().isForbidden());
  }

  @Test
  void loginWithDisallowedOriginGetsCorsRejection() throws Exception {
    mockMvc
        .perform(
            post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ADMIN_LOGIN_BODY)
                .header("Origin", "https://evil.example.com"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
  }
}
