package de.foerderverein.kasse.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Zentrale Ablageorte der Anwendung. Bewusst ein einfaches Punktverzeichnis unter dem
 * Benutzerverzeichnis statt plattformspezifischer AppData/Library-Pfade - reicht fuer
 * eine Ein-Benutzer-Desktop-Anwendung und ist auf Windows wie macOS identisch herzuleiten. */
public final class AppConfig {

    private static final Path APP_DIR = Path.of(System.getProperty("user.home"), ".foerderverein-kasse");

    private AppConfig() {
    }

    public static Path appDir() {
        ensureExists(APP_DIR);
        return APP_DIR;
    }

    public static Path databaseFile() {
        return appDir().resolve("kasse.db");
    }

    public static Path hbciPassportFile() {
        return appDir().resolve("hbci-passport.dat");
    }

    private static void ensureExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Konnte Anwendungsverzeichnis nicht anlegen: " + dir, e);
        }
    }
}
