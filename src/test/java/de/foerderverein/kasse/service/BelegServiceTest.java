package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class BelegServiceTest {

    private BelegService service;
    private long buchungId;

    @BeforeEach
    void setUp() {
        Database database = TestDatabase.create();
        service = new BelegService(new BelegRepository(database));
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
    void pdfWirdAkzeptiert() throws IOException {
        Path datei = Files.createTempFile("beleg-test", ".pdf");
        Files.write(datei, new byte[] {1, 2, 3});

        service.hochladen(buchungId, datei.toFile());

        Beleg geladen = service.findByBuchungId(buchungId).orElseThrow();
        assertEquals("pdf", geladen.getDateityp());
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
