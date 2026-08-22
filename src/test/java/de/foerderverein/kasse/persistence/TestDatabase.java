package de.foerderverein.kasse.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Baut fuer jeden Test eine frische, migrierte SQLite-Datei in einem temporaeren Verzeichnis auf. */
public final class TestDatabase {

    private TestDatabase() {
    }

    public static Database create() {
        try {
            Path dir = Files.createTempDirectory("kasse-test");
            Database database = new Database(dir.resolve("test.db"));
            database.migrate();
            return database;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
