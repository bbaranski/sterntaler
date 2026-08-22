package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Geschaeftsjahr;

class GeschaeftsjahrRepositoryTest {

    @Test
    void insertUndFindAll() {
        GeschaeftsjahrRepository repository = new GeschaeftsjahrRepository(TestDatabase.create());

        repository.save(neuesJahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

        var alle = repository.findAll();
        assertEquals(1, alle.size());
        assertEquals("2026", alle.get(0).getBezeichnung());
    }

    @Test
    void startsaldoWirdGespeichertUndGeladen() {
        GeschaeftsjahrRepository repository = new GeschaeftsjahrRepository(TestDatabase.create());
        Geschaeftsjahr jahr = neuesJahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        jahr.setStartsaldoCent(12345);

        repository.save(jahr);

        var geladen = repository.findAll().get(0);
        assertEquals(12345, geladen.getStartsaldoCent());
    }

    @Test
    void findAbgeschlossenesJahrFuerFindetNurAbgeschlosseneJahre() {
        GeschaeftsjahrRepository repository = new GeschaeftsjahrRepository(TestDatabase.create());
        Geschaeftsjahr offen = neuesJahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        repository.save(offen);

        assertTrue(repository.findAbgeschlossenesJahrFuer(LocalDate.of(2026, 6, 15)).isEmpty());

        offen.setAbgeschlossen(true);
        repository.save(offen);

        var gefunden = repository.findAbgeschlossenesJahrFuer(LocalDate.of(2026, 6, 15));
        assertTrue(gefunden.isPresent());
        assertEquals("2026", gefunden.get().getBezeichnung());

        assertFalse(repository.findAbgeschlossenesJahrFuer(LocalDate.of(2025, 6, 15)).isPresent());
    }

    private Geschaeftsjahr neuesJahr(String bezeichnung, LocalDate start, LocalDate end) {
        Geschaeftsjahr g = new Geschaeftsjahr();
        g.setBezeichnung(bezeichnung);
        g.setStartDatum(start);
        g.setEndDatum(end);
        return g;
    }
}
