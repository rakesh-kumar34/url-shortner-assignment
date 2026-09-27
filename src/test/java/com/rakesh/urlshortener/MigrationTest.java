package com.rakesh.urlshortener;

import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MigrationTest {
    @Test void upgradesPopulatedV1WithoutLosingOldLinksOrCounts() throws Exception {
        String url = "jdbc:h2:mem:migration_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("1").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO links(code,destination,title,created_at,total_clicks) VALUES ('legacy','https://example.com','Legacy',CURRENT_TIMESTAMP,12)");
        }
        Flyway.configure().dataSource(url, "sa", "").load().migrate();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var statement = connection.createStatement();
                var rows = statement.executeQuery("SELECT total_clicks,expires_at,disabled_at FROM links WHERE code='legacy'")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getLong(1)).isEqualTo(12);
            assertThat(rows.getObject(2)).isNull();
            assertThat(rows.getObject(3)).isNull();
        }
    }
}
