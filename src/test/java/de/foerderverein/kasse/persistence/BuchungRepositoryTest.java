package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;

class BuchungRepositoryTest {

    private BuchungRepository buchungRepository;
    private Konto konto;

    @BeforeEach
    void setUp() {
        Database database = TestDatabase.create();
        buchungRepository = new BuchungRepository(database);
        konto = new KontoRepository(database).save(neuesKonto());
    }

    @Test
    void insertUndFindAll() {
        Buchung buchung = neueBuchung(BuchungsQuelle.MANUELL, null);
        buchungRepository.insert(buchung);

        var alle = buchungRepository.findAll();

        assertEquals(1, alle.size());
        assertEquals(buchung.getVerwendungszweck(), alle.get(0).getVerwendungszweck());
    }

    @Test
    void existsByHbciHashErkenntDuplikate() {
        Buchung buchung = neueBuchung(BuchungsQuelle.HBCI_IMPORT, "hash-123");
        assertFalse(buchungRepository.existsByHbciHash(konto.getId(), "hash-123"));

        buchungRepository.insert(buchung);

        assertTrue(buchungRepository.existsByHbciHash(konto.getId(), "hash-123"));
    }

    @Test
    void mitgliedZuordnungWirdPersistiert() {
        Database database = TestDatabase.create();
        buchungRepository = new BuchungRepository(database);
        konto = new KontoRepository(database).save(neuesKonto());
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        new MitgliedRepository(database).save(mitglied);

        Buchung buchung = neueBuchung(BuchungsQuelle.MANUELL, null);
        buchung.setMitgliedId(mitglied.getId());
        buchungRepository.insert(buchung);

        var geladen = buchungRepository.findAll().get(0);
        assertEquals(mitglied.getId(), geladen.getMitgliedId());
    }

    @Test
    void aktivitaetZuordnungWirdPersistiert() {
        Database database = TestDatabase.create();
        buchungRepository = new BuchungRepository(database);
        konto = new KontoRepository(database).save(neuesKonto());
        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setName("Sankt Martin");
        new AktivitaetRepository(database).save(aktivitaet);

        Buchung buchung = neueBuchung(BuchungsQuelle.MANUELL, null);
        buchung.setAktivitaetId(aktivitaet.getId());
        buchungRepository.insert(buchung);

        var geladen = buchungRepository.findAll().get(0);
        assertEquals(aktivitaet.getId(), geladen.getAktivitaetId());
    }

    @Test
    void stornoMarkiertStattZuLoeschen() {
        Buchung buchung = neueBuchung(BuchungsQuelle.MANUELL, null);
        buchungRepository.insert(buchung);

        buchungRepository.storno(buchung.getId());

        var alle = buchungRepository.findAll();
        assertEquals(1, alle.size());
        assertTrue(alle.get(0).isStorniert());
    }

    private Buchung neueBuchung(BuchungsQuelle quelle, String hash) {
        Buchung buchung = new Buchung();
        buchung.setKontoId(konto.getId());
        buchung.setBuchungsdatum(LocalDate.of(2026, 1, 15));
        buchung.setBetragEuro(new java.math.BigDecimal("42.50"));
        buchung.setVerwendungszweck("Testbuchung");
        buchung.setQuelle(quelle);
        buchung.setHbciImportHash(hash);
        return buchung;
    }

    private Konto neuesKonto() {
        Konto konto = new Konto();
        konto.setBezeichnung("Vereinskonto");
        konto.setIban("DE02120300000000202051");
        return konto;
    }
}
