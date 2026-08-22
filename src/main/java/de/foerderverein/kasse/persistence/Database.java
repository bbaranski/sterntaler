package de.foerderverein.kasse.persistence;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Verbindungsfabrik fuer die eingebettete SQLite-Datenbank inkl. Flyway-Migration beim Start. */
public final class Database {

    private static final Logger LOG = LoggerFactory.getLogger(Database.class);

    private final String jdbcUrl;

    public Database(Path databaseFile) {
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
    }

    /** Fuehrt beim ersten Aufruf ausstehende Flyway-Migrationen aus. Danebenliegende
     * Aufrufe sind gefahrlos, Flyway erkennt bereits angewendete Migrationen. */
    public void migrate() {
        LOG.info("Fuehre Datenbank-Migrationen aus: {}", jdbcUrl);
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl, null, null)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    public Connection connection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
        return connection;
    }
}
