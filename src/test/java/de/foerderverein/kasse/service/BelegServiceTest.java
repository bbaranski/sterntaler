package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Beleg;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.BelegRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class BelegServiceTest {

    private BelegService service;
    private BuchungRepository buchungRepository;
    private Path belegeVerzeichnis;
    private Buchung buchung;

    @BeforeEach
    void setUp() throws IOException {
        Database database = TestDatabase.create();
        belegeVerzeichnis = Files.createTempDirectory("kasse-test-belege");
        GeschaeftsjahrService geschaeftsjahrService = new GeschaeftsjahrService(new GeschaeftsjahrRepository(database));
        service = new BelegService(new BelegRepository(database), geschaeftsjahrService, belegeVerzeichnis);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        buchungRepository = new BuchungRepository(database);
        buchung = neueBuchung(konto.getId());
        buchung.setRechnungsnummer("RE-2026-042");
        buchungRepository.insert(buchung);
    }

    @Test
    void unerlaubteEndungWirdAbgelehnt() throws IOException {
        Path datei = Files.createTempFile("beleg-test", ".txt");
        Files.writeString(datei, "kein Beleg");

        assertThrows(IllegalArgumentException.class, () -> service.hochladen(buchung, datei.toFile()));
    }

    @Test
    void uploadOhneRechnungsnummerWirdAbgelehnt() throws IOException {
        buchung.setRechnungsnummer(null);
        Path datei = Files.createTempFile("beleg-test", ".pdf");
        Files.write(datei, new byte[] {1});

        assertThrows(IllegalStateException.class, () -> service.hochladen(buchung, datei.toFile()));
    }

    @Test
    void dateinameSetztSichAusGeschaeftsjahrRechnungsnummerUndOriginalnameZusammen() throws IOException {
        Path datei = Files.createTempFile("beleg-test", ".pdf");
        byte[] inhalt = {1, 2, 3};
        Files.write(datei, inhalt);

        Beleg beleg = service.hochladen(buchung, datei.toFile());

        Beleg geladen = service.findByBuchungId(buchung.getId()).orElseThrow();
        assertEquals("pdf", geladen.getDateityp());
        assertEquals("ohne-Geschaeftsjahr_RE-2026-042_" + datei.getFileName(), geladen.getSpeicherpfad());

        Path gespeicherteDatei = service.pfadZuDatei(beleg);
        assertTrue(Files.exists(gespeicherteDatei));
        assertArrayEquals(inhalt, Files.readAllBytes(gespeicherteDatei));
    }

    @Test
    void grossschreibungDerEndungWirdAkzeptiertUndKleingeschriebenGespeichert() throws IOException {
        Path datei = Files.createTempFile("beleg-test", ".PDF");
        Files.write(datei, new byte[] {1});

        service.hochladen(buchung, datei.toFile());

        Beleg geladen = service.findByBuchungId(buchung.getId()).orElseThrow();
        assertEquals("pdf", geladen.getDateityp());
    }

    @Test
    void jpgUndJpegWerdenAkzeptiert() throws IOException {
        Path jpg = Files.createTempFile("beleg-test", ".jpg");
        Files.write(jpg, new byte[] {1});
        Path jpeg = Files.createTempFile("beleg-test", ".jpeg");
        Files.write(jpeg, new byte[] {1});

        service.hochladen(buchung, jpg.toFile());
        assertEquals("jpg", service.findByBuchungId(buchung.getId()).orElseThrow().getDateityp());

        service.hochladen(buchung, jpeg.toFile());
        assertEquals("jpeg", service.findByBuchungId(buchung.getId()).orElseThrow().getDateityp());
    }

    @Test
    void ersetzenMitAndererEndungEntferntDieAlteDatei() throws IOException {
        Path altesPdf = Files.createTempFile("beleg-test", ".pdf");
        Files.write(altesPdf, new byte[] {1});
        Beleg alterBeleg = service.hochladen(buchung, altesPdf.toFile());
        Path alteDatei = service.pfadZuDatei(alterBeleg);
        assertTrue(Files.exists(alteDatei));

        Path neuesJpg = Files.createTempFile("beleg-test", ".jpg");
        Files.write(neuesJpg, new byte[] {2});
        service.hochladen(buchung, neuesJpg.toFile());

        assertFalse(Files.exists(alteDatei));
    }

    @Test
    void uploadMitDoppelterRechnungsnummerFuerAndereBuchungWirdAbgelehnt() throws IOException {
        Buchung zweiteBuchung = neueBuchung(buchung.getKontoId());
        zweiteBuchung.setRechnungsnummer(buchung.getRechnungsnummer());
        buchungRepository.insert(zweiteBuchung);

        Path datei = Files.createTempFile("beleg-test", ".pdf");
        Files.write(datei, new byte[] {1});
        service.hochladen(buchung, datei.toFile());

        // Gleiches Geschaeftsjahr, gleiche (versehentlich doppelt vergebene) Rechnungsnummer,
        // gleicher Originaldateiname -> derselbe Zieldateiname wie bei der ersten Buchung.
        assertThrows(IllegalStateException.class, () -> service.hochladen(zweiteBuchung, datei.toFile()));
    }

    @Test
    void entfernenLoeschtDateiUndDatenbankzeile() throws IOException {
        Path datei = Files.createTempFile("beleg-test", ".pdf");
        Files.write(datei, new byte[] {1});
        Beleg beleg = service.hochladen(buchung, datei.toFile());
        Path gespeicherteDatei = service.pfadZuDatei(beleg);
        assertTrue(Files.exists(gespeicherteDatei));

        service.entfernen(buchung.getId());

        assertFalse(Files.exists(gespeicherteDatei));
        assertTrue(service.findByBuchungId(buchung.getId()).isEmpty());
    }

    @Test
    void entfernenOhneVorhandenenBelegIstEinSicheresNoop() {
        service.entfernen(buchung.getId());

        assertTrue(service.findByBuchungId(buchung.getId()).isEmpty());
    }

    private Buchung neueBuchung(Long kontoId) {
        Buchung buchung = new Buchung();
        buchung.setKontoId(kontoId);
        buchung.setBuchungsdatum(LocalDate.of(2026, 1, 15));
        buchung.setBetragEuro(new BigDecimal("42.50"));
        buchung.setVerwendungszweck("Testbuchung");
        buchung.setQuelle(BuchungsQuelle.MANUELL);
        return buchung;
    }

    private Konto neuesKonto() {
        Konto konto = new Konto();
        konto.setBezeichnung("Vereinskonto");
        konto.setIban("DE02120300000000202051");
        return konto;
    }
}
