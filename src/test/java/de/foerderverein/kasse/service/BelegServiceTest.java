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
import de.foerderverein.kasse.persistence.BelegDateiMigration;
import de.foerderverein.kasse.persistence.BelegRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class BelegServiceTest {

    private BelegService service;
    private Path belegeVerzeichnis;
    private long buchungId;

    @BeforeEach
    void setUp() throws IOException {
        Database database = TestDatabase.create();
        belegeVerzeichnis = Files.createTempDirectory("kasse-test-belege");
        BelegDateiMigration.migriereFallsNoetig(database, belegeVerzeichnis);
        service = new BelegService(new BelegRepository(database), belegeVerzeichnis);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        Buchung buchung = neueBuchung(konto.getId());
        new BuchungRepository(database).insert(buchung);
        buchungId = buchung.getId();
    }

    @Test
    void unerlaubteEndungWirdAbgelehnt() throws IOException {
        Path datei = Files.createTempFile("beleg-test", ".txt");
        Files.writeString(datei, "kein Beleg");

        assertThrows(IllegalArgumentException.class, () -> service.hochladen(buchungId, datei.toFile()));
    }

    @Test
    void pdfWirdAlsDateiImBelegeVerzeichnisGespeichert() throws IOException {
        Path datei = Files.createTempFile("beleg-test", ".pdf");
        byte[] inhalt = {1, 2, 3};
        Files.write(datei, inhalt);

        Beleg beleg = service.hochladen(buchungId, datei.toFile());

        Beleg geladen = service.findByBuchungId(buchungId).orElseThrow();
        assertEquals("pdf", geladen.getDateityp());
        assertEquals(buchungId + "_" + datei.getFileName().toString(), geladen.getSpeicherpfad());

        Path gespeicherteDatei = service.pfadZuDatei(beleg);
        assertTrue(Files.exists(gespeicherteDatei));
        assertArrayEquals(inhalt, Files.readAllBytes(gespeicherteDatei));
    }

    @Test
    void grossschreibungDerEndungWirdAkzeptiertUndKleingeschriebenGespeichert() throws IOException {
        Path datei = Files.createTempFile("beleg-test", ".PDF");
        Files.write(datei, new byte[] {1});

        service.hochladen(buchungId, datei.toFile());

        Beleg geladen = service.findByBuchungId(buchungId).orElseThrow();
        assertEquals("pdf", geladen.getDateityp());
    }

    @Test
    void jpgUndJpegWerdenAkzeptiert() throws IOException {
        Path jpg = Files.createTempFile("beleg-test", ".jpg");
        Files.write(jpg, new byte[] {1});
        Path jpeg = Files.createTempFile("beleg-test", ".jpeg");
        Files.write(jpeg, new byte[] {1});

        service.hochladen(buchungId, jpg.toFile());
        assertEquals("jpg", service.findByBuchungId(buchungId).orElseThrow().getDateityp());

        service.hochladen(buchungId, jpeg.toFile());
        assertEquals("jpeg", service.findByBuchungId(buchungId).orElseThrow().getDateityp());
    }

    @Test
    void ersetzenMitAndererEndungEntferntDieAlteDatei() throws IOException {
        Path altesPdf = Files.createTempFile("beleg-test", ".pdf");
        Files.write(altesPdf, new byte[] {1});
        Beleg alterBeleg = service.hochladen(buchungId, altesPdf.toFile());
        Path alteDatei = service.pfadZuDatei(alterBeleg);
        assertTrue(Files.exists(alteDatei));

        Path neuesJpg = Files.createTempFile("beleg-test", ".jpg");
        Files.write(neuesJpg, new byte[] {2});
        service.hochladen(buchungId, neuesJpg.toFile());

        assertFalse(Files.exists(alteDatei));
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
