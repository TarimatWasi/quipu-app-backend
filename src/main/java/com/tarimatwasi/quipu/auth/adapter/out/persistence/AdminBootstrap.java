package com.tarimatwasi.quipu.auth.adapter.out.persistence;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN from ADMIN_* environment variables when the system has no active one, so
 * no credential ever lives in the repository. The account is forced to change its password.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);
  private static final int MIN_PASSWORD_LENGTH = 8;

  private final JdbcTemplate jdbc;
  private final PasswordEncoder passwordEncoder;
  private final String documentNumber;
  private final String email;
  private final String initialPassword;
  private final boolean required;

  public AdminBootstrap(
      JdbcTemplate jdbc,
      PasswordEncoder passwordEncoder,
      @Value("${app.admin.document-number:}") String documentNumber,
      @Value("${app.admin.email:}") String email,
      @Value("${app.admin.initial-password:}") String initialPassword,
      @Value("${app.admin.required:false}") boolean required) {
    this.jdbc = jdbc;
    this.passwordEncoder = passwordEncoder;
    this.documentNumber = documentNumber;
    this.email = email;
    this.initialPassword = initialPassword;
    this.required = required;
  }

  @Override
  public void run(ApplicationArguments args) {
    int admins = countAdmins();
    boolean anySet = !documentNumber.isBlank() || !email.isBlank() || !initialPassword.isBlank();
    if (admins > 0) {
      if (anySet) {
        LOG.warn(
            "ADMIN_* variables are still set but an active ADMIN already exists: remove them, "
                + "otherwise the initial password would recreate an ADMIN if the last one is deleted");
      }
      return;
    }
    if (!anySet && required) {
      throw new IllegalStateException(
          "No active ADMIN user exists and ADMIN_DOCUMENT_NUMBER / ADMIN_EMAIL / "
              + "ADMIN_INITIAL_PASSWORD are not set: nobody could access the system");
    }
    if (!anySet) {
      LOG.warn(
          "No active ADMIN user exists and ADMIN_DOCUMENT_NUMBER / ADMIN_EMAIL / ADMIN_INITIAL_PASSWORD are not set");
      return;
    }
    if (documentNumber.isBlank() || email.isBlank() || initialPassword.isBlank()) {
      throw new IllegalStateException(
          "ADMIN_DOCUMENT_NUMBER, ADMIN_EMAIL and ADMIN_INITIAL_PASSWORD must be set together");
    }
    if (initialPassword.length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalStateException(
          "ADMIN_INITIAL_PASSWORD must have at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    try {
      jdbc.update(
          "INSERT INTO users (id, email, document_type, document_number, password_hash, role, "
              + "must_change_password, status) VALUES (?, ?, 'DNI', ?, ?, 'ADMIN', TRUE, 'ACTIVE')",
          UUID.randomUUID(),
          email,
          documentNumber,
          passwordEncoder.encode(initialPassword));
      LOG.info("Initial ADMIN user created (password change required on first login)");
    } catch (DuplicateKeyException e) {
      if (countAdmins() == 0) {
        throw new IllegalStateException(
            "Cannot create the initial ADMIN: ADMIN_EMAIL or ADMIN_DOCUMENT_NUMBER already belongs "
                + "to another user (not an active ADMIN)",
            e);
      }
      LOG.info("Initial ADMIN was created concurrently by another instance");
    }
  }

  private int countAdmins() {
    // same rule as UserAccount.isInactive(): an INACTIVE ADMIN cannot log in
    Integer admins =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM users WHERE role = 'ADMIN' AND status <> 'INACTIVE'",
            Integer.class);
    return admins == null ? 0 : admins;
  }
}
