package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Briefvorlage;
import de.foerderverein.kasse.persistence.BriefvorlageRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.TestDatabase;

class BriefvorlageServiceTest {

    private BriefvorlageService service;
    private Path vorlagenVerzeichnis;

    @BeforeEach
    void setUp() throws IOException {
        Database database = TestDatabase.create();
        vorlagenVerzeichnis = Files.createTempDirectory("kasse-test-vorlagen");
        service = new BriefvorlageService(new BriefvorlageRepository(database), vorlagenVerzeichnis);
    }

    @Test
    void speichertVorlageUndKopiertWordDatei() throws IOException {
        Path docx = docxDatei("vorlage", "Hallo {{Vorname}}");

        Briefvorlage gespeichert = service.speichern(neueVorlage("Vorstandssitzung"), docx.toFile());

        assertEquals(docx.getFileName().toString(), gespeichert.getDateiname());
        assertTrue(Files.exists(service.pfadZuDatei(gespeichert)));
        assertEquals(1, service.findAll().size());
    }

    @Test
    void speichernOhneWordDateiWirdAbgelehnt() {
        assertThrows(IllegalArgumentException.class, () -> service.speichern(neueVorlage("Ohne Datei"), null));
    }

    @Test
    void nurDocxWirdAkzeptiert() throws IOException {
        Path keinDocx = Files.createTempFile("vorlage", ".txt");
        Files.writeString(keinDocx, "kein Word");

        assertThrows(IllegalArgumentException.class,
                () -> service.speichern(neueVorlage("Falscher Typ"), keinDocx.toFile()));
    }

    @Test
    void bearbeitenOhneNeueDateiBehaeltBestehendeDatei() throws IOException {
        Briefvorlage vorlage = service.speichern(neueVorlage("Vorstandssitzung"),
                docxDatei("vorlage", "Hallo {{Vorname}}").toFile());
        Path dateiVorher = service.pfadZuDatei(vorlage);

        vorlage.setBetreff("Neuer Betreff");
        service.speichern(vorlage, null);

        Briefvorlage geladen = service.findAll().get(0);
        assertEquals("Neuer Betreff", geladen.getBetreff());
        assertEquals(dateiVorher.getFileName().toString(), geladen.getSpeicherpfad());
        assertTrue(Files.exists(dateiVorher));
    }

    @Test
    void ersetzenDerWordDateiEntferntDieAlteDatei() throws IOException {
        Briefvorlage vorlage = service.speichern(neueVorlage("Vorstandssitzung"),
                docxDatei("alt", "Alt {{Vorname}}").toFile());
        Path alteDatei = service.pfadZuDatei(vorlage);

        service.speichern(vorlage, docxDatei("neu", "Neu {{Vorname}}").toFile());

        assertFalse(Files.exists(alteDatei));
        assertTrue(Files.exists(service.pfadZuDatei(vorlage)));
    }

    @Test
    void entfernenLoeschtDateiUndDatenbankzeile() throws IOException {
        Briefvorlage vorlage = service.speichern(neueVorlage("Vorstandssitzung"),
                docxDatei("vorlage", "Hallo {{Vorname}}").toFile());
        Path datei = service.pfadZuDatei(vorlage);

        service.entfernen(vorlage);

        assertFalse(Files.exists(datei));
        assertTrue(service.findAll().isEmpty());
    }

    private Briefvorlage neueVorlage(String name) {
        Briefvorlage vorlage = new Briefvorlage();
        vorlage.setName(name);
        vorlage.setBetreff("Einladung");
        vorlage.setBrieftext("Tagesordnung folgt.");
        return vorlage;
    }

    private Path docxDatei(String praefix, String inhalt) throws IOException {
        Path datei = Files.createTempFile(praefix, ".docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText(inhalt);
            try (var out = Files.newOutputStream(datei)) {
                doc.write(out);
            }
        }
        return datei;
    }
}
