package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Beleg;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Konto;

class BelegRepositoryTest {

    private BelegRepository belegRepository;
    private Path belegeVerzeichnis;
    private long buchungId;

    @BeforeEach
    void setUp() {
        Database database = TestDatabase.create();
        belegeVerzeichnis = neuesTempVerzeichnis();
        BelegDateiMigration.migriereFallsNoetig(database, belegeVerzeichnis);
        belegRepository = new BelegRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Buchung buchung = neueBuchung(konto.getId());
        buchungRepository.insert(buchung);
        buchungId = buchung.getId();
    }

    @Test
    void speichernUndFindByBuchungId() {
        belegRepository.speichern(neuerBeleg(buchungId, "Rechnung.pdf", "pdf", "482_Rechnung.pdf"));

        Beleg geladen = belegRepository.findByBuchungId(buchungId).orElseThrow();

        assertEquals("Rechnung.pdf", geladen.getDateiname());
        assertEquals("pdf", geladen.getDateityp());
        assertEquals("482_Rechnung.pdf", geladen.getSpeicherpfad());
    }

    @Test
    void ersetzenUeberschreibtBestehendenBeleg() {
        Beleg erster = belegRepository.speichern(neuerBeleg(buchungId, "Alt.pdf", "pdf", "1_Alt.pdf"));
        Beleg zweiter = belegRepository.speichern(neuerBeleg(buchungId, "Neu.jpg", "jpg", "1_Neu.jpg"));

        assertEquals(erster.getId(), zweiter.getId());
        Beleg geladen = belegRepository.findByBuchungId(buchungId).orElseThrow();
        assertEquals("Neu.jpg", geladen.getDateiname());
        assertEquals("1_Neu.jpg", geladen.getSpeicherpfad());
    }

    @Test
    void findAllEnthaeltSpeicherpfad() {
        belegRepository.speichern(neuerBeleg(buchungId, "Rechnung.pdf", "pdf", "482_Rechnung.pdf"));

        Beleg gefunden = belegRepository.findAll().get(buchungId);

        assertTrue(gefunden != null);
        assertEquals("Rechnung.pdf", gefunden.getDateiname());
        assertEquals("482_Rechnung.pdf", gefunden.getSpeicherpfad());
    }

    private Beleg neuerBeleg(long buchungId, String dateiname, String dateityp, String speicherpfad) {
        Beleg beleg = new Beleg();
        beleg.setBuchungId(buchungId);
        beleg.setDateiname(dateiname);
        beleg.setDateityp(dateityp);
        beleg.setSpeicherpfad(speicherpfad);
        beleg.setHochgeladenAm(LocalDateTime.now());
        return beleg;
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

    private Path neuesTempVerzeichnis() {
        try {
            return Files.createTempDirectory("kasse-test-belege");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
