package com.tarimatwasi.quipu.bff.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

/**
 * The SameSite attribute of the session cookie comes from {@code app.session.same-site}: a product
 * whose frontend lives on another registrable domain can choose {@code none}; the default is Lax.
 */
@SpringBootTest(
    properties = {"app.cors.allowed-origin=http://localhost:3000", "app.session.same-site=none"})
@AutoConfigureMockMvc
@ImportTestcontainers(PostgresContainers.class)
class SessionCookiePropertiesTest {

  @Autowired MockMvc mockMvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder passwordEncoder;

  @BeforeEach
  void adminExists() {
    jdbc.update("DELETE FROM users");
    jdbc.update(
        "INSERT INTO users (id, email, document_type, document_number, password_hash, role,"
            + " must_change_password, status)"
            + " VALUES (?, 'admin@example.test', 'DNI', '00000000', ?, 'ADMIN', FALSE, 'ACTIVE')",
        UUID.randomUUID(),
        passwordEncoder.encode("Temporal123!"));
  }

  @Test
  void theConfiguredSameSiteValueIsUsed() throws Exception {
    var setCookie =
        mockMvc
            .perform(
                post("/bff/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"documentType":"DNI","documentNumber":"00000000","password":"Temporal123!"}
                        """))
            .andReturn()
            .getResponse()
            .getHeader("Set-Cookie");

    assertThat(setCookie).contains("; SameSite=None").contains("; Secure").contains("; HttpOnly");
  }
}
