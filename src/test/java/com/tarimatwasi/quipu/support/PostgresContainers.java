package com.tarimatwasi.quipu.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Shared PostgreSQL for integration tests (BE-SPR-TST-03); import with @ImportTestcontainers. */
public interface PostgresContainers {

  @Container @ServiceConnection
  PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
}
