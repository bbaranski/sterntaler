package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
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
    private long buchungId;

    @BeforeEach
    void setUp() {
        Database database = TestDatabase.create();
        belegRepository = new BelegRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Buchung buchung = neueBuchung(konto.getId());
        buchungRepository.insert(buchung);
        buchungId = buchung.getId();
    }

    @Test
    void speichernUndFindByBuchungId() {
        byte[] inhalt = {1, 2, 3, 4};
        belegRepository.speichern(neuerBeleg(buchungId, "Rechnung.pdf", "pdf", inhalt));

        Beleg geladen = belegRepository.findByBuchungId(buchungId).orElseThrow();

        assertEquals("Rechnung.pdf", geladen.getDateiname());
        assertEquals("pdf", geladen.getDateityp());
        assertArrayEquals(inhalt, geladen.getInhalt());
    }

    @Test
    void ersetzenUeberschreibtBestehendenBeleg() {
        Beleg erster = belegRepository.speichern(neuerBeleg(buchungId, "Alt.pdf", "pdf", new byte[] {1}));
        Beleg zweiter = belegRepository.speichern(neuerBeleg(buchungId, "Neu.jpg", "jpg", new byte[] {2, 2}));

        assertEquals(erster.getId(), zweiter.getId());
        Beleg geladen = belegRepository.findByBuchungId(buchungId).orElseThrow();
        assertEquals("Neu.jpg", geladen.getDateiname());
        assertArrayEquals(new byte[] {2, 2}, geladen.getInhalt());
    }

    @Test
    void findAllMetadatenEnthaeltKeinenInhalt() {
        belegRepository.speichern(neuerBeleg(buchungId, "Rechnung.pdf", "pdf", new byte[] {9, 9}));

        Beleg metadaten = belegRepository.findAllMetadaten().get(buchungId);

        assertEquals("Rechnung.pdf", metadaten.getDateiname());
        assertNull(metadaten.getInhalt());
    }

    private Beleg neuerBeleg(long buchungId, String dateiname, String dateityp, byte[] inhalt) {
        Beleg beleg = new Beleg();
        beleg.setBuchungId(buchungId);
        beleg.setDateiname(dateiname);
        beleg.setDateityp(dateityp);
        beleg.setInhalt(inhalt);
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
}
