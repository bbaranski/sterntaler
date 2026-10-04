package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Briefvorlage;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.Verein;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.TestDatabase;
import de.foerderverein.kasse.persistence.VereinRepository;

class SerienbriefServiceTest {

    private SerienbriefService service;
    private Path vorlagenVerzeichnis;
    private Briefvorlage vorlage;

    @BeforeEach
    void setUp() throws IOException {
        Database database = TestDatabase.create();
        vorlagenVerzeichnis = Files.createTempDirectory("kasse-test-vorlagen");

        VereinRepository vereinRepository = new VereinRepository(database);
        Verein verein = new Verein();
        verein.setName("Förderverein Musterschule e. V.");
        verein.setOrt("Moers");
        vereinRepository.save(verein);

        service = new SerienbriefService(vorlagenVerzeichnis, vereinRepository);

        vorlage = new Briefvorlage();
        vorlage.setName("Vorstandssitzung");
        vorlage.setBetreff("Einladung zur Vorstandssitzung");
        vorlage.setBrieftext("Tagesordnung folgt.");
        vorlage.setSpeicherpfad("vorlage.docx");
        schreibeVorlage(vorlagenVerzeichnis.resolve("vorlage.docx"));
    }

    /** Baut eine minimale Word-Vorlage. Der Platzhalter {{Vorname}} wird bewusst ueber zwei Runs
     * verteilt erzeugt - genau so, wie Word Text intern haeufig zerstueckelt - damit die
     * Run-zusammenfuehrende Ersetzung mitgeprueft wird. */
    private void schreibeVorlage(Path ziel) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph empfaenger = doc.createParagraph();
            empfaenger.createRun().setText("{{Vor");
            empfaenger.createRun().setText("name}} {{Nachname}}");

            doc.createParagraph().createRun().setText("{{Strasse}}, {{Plz}} {{Ort}}");
            doc.createParagraph().createRun().setText("{{VereinsName}}, {{Datum}}");
            doc.createParagraph().createRun().setText("{{Brieftext}}");
            doc.createParagraph().createRun().setText("{{SignaturName}} / {{SignaturRolle}}");
            doc.createParagraph().createRun().setText("{{UnbekannterPlatzhalter}}");

            try (var out = Files.newOutputStream(ziel)) {
                doc.write(out);
            }
        }
    }

    @Test
    void erstelltSammeldruckMitEinerSeiteJeMitglied() throws IOException {
        List<Mitglied> empfaenger = List.of(
                mitglied("Erika", "Musterfrau", "Seminarstr. 6a", "47441", "Moers", "erika@example.org"),
                mitglied("Max", "Mustermann", "Hauptstr. 1", "47441", "Moers", null));

        Path ziel = Files.createTempDirectory("kasse-serienbrief-test").resolve("serienbrief.docx");
        service.erstelleSammeldruckDocx(vorlage, empfaenger, "Tagesordnung: 1. Begrüßung", "F. Thies",
                "Schriftführerin", ziel);

        assertTrue(Files.exists(ziel));
        try (XWPFDocument erzeugt = new XWPFDocument(Files.newInputStream(ziel))) {
            String text = String.join("\n", erzeugt.getParagraphs().stream().map(XWPFParagraph::getText).toList());

            assertTrue(text.contains("Erika Musterfrau"), text);
            assertTrue(text.contains("Max Mustermann"), text);
            assertTrue(text.contains("Seminarstr. 6a, 47441 Moers"), text);
            assertTrue(text.contains("Hauptstr. 1, 47441 Moers"), text);
            assertTrue(text.contains("Förderverein Musterschule e. V."), text);
            assertTrue(text.contains("Tagesordnung: 1. Begrüßung"), text);
            assertTrue(text.contains("F. Thies / Schriftführerin"), text);
            // Unbekannte Platzhalter bleiben unveraendert stehen statt zu verschwinden.
            assertTrue(text.contains("{{UnbekannterPlatzhalter}}"), text);

            long seitenumbrueche = erzeugt.getParagraphs().stream()
                    .filter(p -> p.getCTP().xmlText().contains("type=\"page\""))
                    .count();
            assertEquals(1, seitenumbrueche);
        }
    }

    /** Ein blosses "\n" in einem w:t-Element stellt Word nicht als Zeilenumbruch dar - mehrzeilige
     * Brieftexte (Tagesordnungen!) muessen daher als echte w:br-Umbrueche landen. */
    @Test
    void mehrzeiligerBrieftextWirdMitEchtenWordUmbruechenGeschrieben() throws IOException {
        List<Mitglied> empfaenger = List.of(
                mitglied("Erika", "Musterfrau", "Seminarstr. 6a", "47441", "Moers", "erika@example.org"));

        Path ziel = Files.createTempDirectory("kasse-serienbrief-test").resolve("mehrzeilig.docx");
        service.erstelleSammeldruckDocx(vorlage, empfaenger, "Zeile eins\nZeile zwei\nZeile drei", "F. Thies",
                "Schriftführerin", ziel);

        try (XWPFDocument erzeugt = new XWPFDocument(Files.newInputStream(ziel))) {
            XWPFParagraph brieftextAbsatz = erzeugt.getParagraphs().stream()
                    .filter(p -> p.getText().contains("Zeile eins"))
                    .findFirst().orElseThrow();
            String xml = brieftextAbsatz.getCTP().xmlText();
            assertEquals(2, xml.split("<w:br/>", -1).length - 1, xml);
            assertTrue(brieftextAbsatz.getText().contains("Zeile drei"), brieftextAbsatz.getText());
        }
    }

    @Test
    void mailEntwurfSammeltAdressenImBccUndMeldetMitgliederOhneEmail() {
        List<Mitglied> empfaenger = List.of(
                mitglied("Erika", "Musterfrau", "Seminarstr. 6a", "47441", "Moers", "erika@example.org"),
                mitglied("Max", "Mustermann", "Hauptstr. 1", "47441", "Moers", null));

        var entwurf = service.erstelleMailEntwurf(empfaenger, "Einladung", "Tagesordnung: 1. Begrüßung",
                "F. Thies", "Schriftführerin");

        assertEquals(List.of("erika@example.org"), entwurf.bccAdressen());
        assertEquals(1, entwurf.ohneEmail().size());
        assertEquals("Max Mustermann", entwurf.ohneEmail().get(0).getVollerName());
        assertEquals("Einladung", entwurf.betreff());
        assertTrue(entwurf.text().startsWith("Liebe Mitglieder,"), entwurf.text());
        assertTrue(entwurf.text().contains("Tagesordnung: 1. Begrüßung"), entwurf.text());
        assertTrue(entwurf.text().contains("Mit freundlichen Grüßen\nF. Thies\nSchriftführerin"), entwurf.text());
    }

    @Test
    void erkenntBekanntePlatzhalterInDerVorlage() throws IOException {
        Path mitPlatzhalter = Files.createTempFile("mit", ".docx");
        schreibeVorlage(mitPlatzhalter);
        Path ohnePlatzhalter = Files.createTempFile("ohne", ".docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Nur Text ohne Platzhalter");
            try (var out = Files.newOutputStream(ohnePlatzhalter)) {
                doc.write(out);
            }
        }

        assertTrue(service.enthaeltBekanntenPlatzhalter(mitPlatzhalter.toFile()));
        assertFalse(service.enthaeltBekanntenPlatzhalter(ohnePlatzhalter.toFile()));
    }

    private Mitglied mitglied(String vorname, String nachname, String strasse, String plz, String ort, String email) {
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname(vorname);
        mitglied.setNachname(nachname);
        mitglied.setStrasse(strasse);
        mitglied.setPlz(plz);
        mitglied.setOrt(ort);
        mitglied.setEmail(email);
        return mitglied;
    }
}
