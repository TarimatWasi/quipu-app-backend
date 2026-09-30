package com.tarimatwasi.quipu.db;

import static org.assertj.core.api.Assertions.assertThat;

import com.tarimatwasi.quipu.support.PostgresContainers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.jdbc.core.JdbcTemplate;

/** BE-SPR-DAT-04 and DAT-06: migrations apply and every table and column has COMMENT ON. */
@SpringBootTest
@ImportTestcontainers(PostgresContainers.class)
class MigrationDocumentationTest {

  private static final String UNDOCUMENTED_TABLES =
      """
      select c.relname from pg_class c
      join pg_namespace n on n.oid = c.relnamespace
      where n.nspname = 'public' and c.relkind = 'r' and c.relname <> 'flyway_schema_history'
        and obj_description(c.oid, 'pg_class') is null
      """;

  private static final String UNDOCUMENTED_COLUMNS =
      """
      select c.relname || '.' || a.attname from pg_attribute a
      join pg_class c on c.oid = a.attrelid
      join pg_namespace n on n.oid = c.relnamespace
      where n.nspname = 'public' and c.relkind = 'r' and c.relname <> 'flyway_schema_history'
        and a.attnum > 0 and not a.attisdropped
        and col_description(c.oid, a.attnum) is null
      """;

  @Autowired JdbcTemplate jdbc;

  @Test
  void migrations_document_every_table_and_column() {
    assertThat(undocumented()).isEmpty();
  }

  @Test
  void detector_flags_a_table_without_comments() {
    jdbc.execute("create table tmp_undocumented (id int)");
    try {
      assertThat(undocumented()).contains("tmp_undocumented", "tmp_undocumented.id");
    } finally {
      jdbc.execute("drop table tmp_undocumented");
    }
  }

  private List<String> undocumented() {
    var all = jdbc.queryForList(UNDOCUMENTED_TABLES, String.class);
    all.addAll(jdbc.queryForList(UNDOCUMENTED_COLUMNS, String.class));
    return all;
  }
}
