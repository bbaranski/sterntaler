package de.foerderverein.kasse.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;


/** Waechter gegen ein veraltetes Handbuch-PDF. Erzeugt wird es von Hand
 * ({@code mvn -o compile exec:java@handbuch}), und genau deshalb kann es vergessen werden -
 * diese Tests schlagen dann fehl, statt dass eine alte Fassung ausgeliefert wird. */
class HandbuchPdfTest {

    private static final Path QUELLE = Path.of("docs", "handbuch.md");
    private static final Path PDF = Path.of("docs", "handbuch.pdf");
    private static final Path PRUEFSUMME = Path.of("docs", "handbuch.md.sha256");

    @Test
    void pdfPasstZumAktuellenHandbuch() throws Exception {
        assertTrue(Files.exists(PDF), PDF + " fehlt - bitte mit \"mvn -o compile exec:java@handbuch\" erzeugen.");

        String vermerkt = Files.readString(PRUEFSUMME, StandardCharsets.UTF_8).trim().split("\\s+")[0];
        String tatsaechlich = HandbuchPdf.sha256(Files.readString(QUELLE, StandardCharsets.UTF_8));

        assertEquals(tatsaechlich, vermerkt,
                "docs/handbuch.md wurde geaendert, ohne das PDF neu zu erzeugen - bitte"
                        + " \"mvn -o compile exec:java@handbuch\" ausfuehren und beides einchecken.");
    }

    /** Das PDF muss auch im Jar landen, sonst findet "Hilfe → Handbuch" es zur Laufzeit nicht. */
    @Test
    void pdfLiegtAlsRessourceImKlassenpfad() throws IOException {
        try (InputStream ressource = getClass()
                .getResourceAsStream("/de/foerderverein/kasse/doc/handbuch.pdf")) {
            assertNotNull(ressource, "Handbuch fehlt im Klassenpfad - siehe <resources> in der pom.xml");
            byte[] anfang = ressource.readNBytes(4);
            assertEquals("%PDF", new String(anfang, StandardCharsets.US_ASCII));
        }
    }
}
