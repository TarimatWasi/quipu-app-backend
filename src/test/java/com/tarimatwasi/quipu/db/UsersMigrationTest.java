package com.tarimatwasi.quipu.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.tarimatwasi.quipu.auth.adapter.out.persistence.AdminBootstrap;
import com.tarimatwasi.quipu.support.PostgresContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The users table and the first-ADMIN bootstrap against a real PostgreSQL. The container is shared
 * by all integration tests, so each test starts from an empty table and runs the bootstrap itself.
 */
@SpringBootTest(
    properties = {
      "app.admin.document-number=12345678",
      "app.admin.email=admin@example.test",
      "app.admin.initial-password=bootstrap-test-pw"
    })
@ImportTestcontainers(PostgresContainers.class)
class UsersMigrationTest {

  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired AdminBootstrap adminBootstrap;

  @BeforeEach
  void emptyUsers() {
    jdbc.update("DELETE FROM users");
  }

  @Test
  void bootstrapCreatesAdminFromConfigWithMustChangePasswordTrue() {
    adminBootstrap.run(new DefaultApplicationArguments());

    var mustChange =
        jdbc.queryForObject(
            "SELECT must_change_password FROM users"
                + " WHERE document_number = '12345678' AND role = 'ADMIN'",
            Boolean.class);
    var hash =
        jdbc.queryForObject(
            "SELECT password_hash FROM users WHERE document_number = '12345678'", String.class);

    assertThat(mustChange).isTrue();
    assertThat(passwordEncoder.matches("bootstrap-test-pw", hash)).isTrue();
  }

  @Test
  void migrationsSeedNoUsers() {
    var users = jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class);

    assertThat(users).isZero();
  }

  @Test
  void loginIdentifierIsUnique() {
    var count =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.table_constraints "
                + "WHERE table_name = 'users' AND constraint_type = 'UNIQUE'",
            Integer.class);

    assertThat(count).isGreaterThanOrEqualTo(1);
  }
}
