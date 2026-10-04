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

    /** Ablageort fuer Belege-Dateien zur gewaehlten Datenbankdatei: ein Geschwisterverzeichnis
     * direkt neben der .db-Datei (z. B. kasse2.db -> kasse2-belege/), damit Belege automatisch
     * mitwandern, wenn die Datenbankdatei kopiert, gesichert oder verschoben wird - anders als
     * appDir() oben, der unabhaengig von der frei waehlbaren Datenbankdatei immer derselbe feste
     * Ordner ist (siehe DatenbankAuswahlDialog). */
    public static Path belegeVerzeichnis(Path databaseFile) {
        return geschwisterVerzeichnis(databaseFile, "-belege");
    }

    /** Ablageort fuer Serienbrief-Word-Vorlagen zur gewaehlten Datenbankdatei - Geschwisterordner
     * neben der .db-Datei, aus demselben Grund wie belegeVerzeichnis(...) oben. */
    public static Path vorlagenVerzeichnis(Path databaseFile) {
        return geschwisterVerzeichnis(databaseFile, "-vorlagen");
    }

    private static Path geschwisterVerzeichnis(Path databaseFile, String suffix) {
        Path absolut = databaseFile.toAbsolutePath();
        String dateiname = absolut.getFileName().toString();
        int punkt = dateiname.lastIndexOf('.');
        String basisname = punkt > 0 ? dateiname.substring(0, punkt) : dateiname;
        Path verzeichnis = absolut.getParent().resolve(basisname + suffix);
        ensureExists(verzeichnis);
        return verzeichnis;
    }

    private static void ensureExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("Konnte Anwendungsverzeichnis nicht anlegen: " + dir, e);
        }
    }
}
