package de.foerderverein.kasse.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.app.Programm;

/** Waechter fuer die Lizenzhinweise. Alle drei Mechanismen dahinter scheitern geraeuschlos:
 * Eine Ressource, die nicht ins Jar wandert, faellt erst dem Anwender auf; eine ungefilterte
 * version.properties liefert die Zeichenfolge "${project.version}" statt einer Nummer; und
 * eine vergessene Neuerzeugung der Drittanbieter-Liste bleibt unbemerkt, bis jemand die
 * Lizenzen prueft. */
class LizenzTest {

    private static final String DOC = "/de/foerderverein/kasse/doc/";

    @Test
    void lizenzUndHinweiseLiegenImKlassenpfad() throws IOException {
        String lizenz = ressource(DOC + "LICENSE");
        assertTrue(lizenz.contains("MIT License"), lizenz);
        assertTrue(lizenz.contains("Copyright (c) 2026 Bastian Baranski"), lizenz);
        assertTrue(lizenz.contains("WITHOUT WARRANTY OF ANY KIND"),
                "Der Gewaehrleistungsausschluss fehlt: " + lizenz);

        String hinweise = ressource(DOC + "DRITTANBIETER.md");
        assertTrue(hinweise.contains("Fremdbibliotheken"), hinweise);
    }

    /** Faellt um, sobald die Fassung nicht mehr aus der pom.xml kommt - etwa weil die
     * Filterung des Ressourcenordners verlorengeht. */
    @Test
    void fassungStimmtMitDerPomUeberein() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);
        Matcher treffer = Pattern.compile("<version>([^<]+)</version>").matcher(pom);
        assertTrue(treffer.find(), "Keine Fassung in der pom.xml gefunden");

        assertEquals(treffer.group(1), Programm.FASSUNG);
        assertEquals("Sterntaler", Programm.NAME);
    }

    /** Sichert den Geltungsbereich der erzeugten Liste: Aufgefuehrt gehoert, was ausgeliefert
     * wird - nicht die Werkzeuge, die nur beim Bauen gebraucht werden. */
    @Test
    void dieListeFuehrtGenauDieAusgelieferteBibliothekenAuf() throws IOException {
        String hinweise = Files.readString(Path.of("DRITTANBIETER.md"), StandardCharsets.UTF_8);

        for (String ausgeliefert : new String[] {"javafx", "sqlite-jdbc", "hbci4j", "openpdf",
                "logback", "jna", "poi"}) {
            assertTrue(hinweise.contains(ausgeliefert),
                    "Ausgelieferte Bibliothek fehlt in der Liste: " + ausgeliefert);
        }
        for (String nurZumBauen : new String[] {"junit", "flexmark", "openhtmltopdf", "pdfbox"}) {
            assertFalse(hinweise.contains(nurZumBauen),
                    "Nur zum Bauen gebraucht, gehoert nicht in die Liste: " + nurZumBauen);
        }
    }

    private String ressource(String pfad) throws IOException {
        try (InputStream quelle = getClass().getResourceAsStream(pfad)) {
            assertNotNull(quelle, "Nicht im Klassenpfad: " + pfad
                    + " - siehe <resources> in der pom.xml");
            return new String(quelle.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
