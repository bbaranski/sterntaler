package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Aktivitaet;

class AktivitaetRepositoryTest {

    @Test
    void insertUndFindAll() {
        AktivitaetRepository repository = new AktivitaetRepository(TestDatabase.create());
        Aktivitaet aktivitaet = neueAktivitaet();

        repository.save(aktivitaet);

        var alle = repository.findAll();
        assertEquals(1, alle.size());
        assertEquals("Sankt Martin", alle.get(0).getName());
    }

    @Test
    void findAllAktivFiltertInaktive() {
        AktivitaetRepository repository = new AktivitaetRepository(TestDatabase.create());
        Aktivitaet aktiv = repository.save(neueAktivitaet());
        Aktivitaet inaktiv = neueAktivitaet();
        inaktiv.setName("Weihnachtsbasar");
        inaktiv.setAktiv(false);
        repository.save(inaktiv);

        var aktive = repository.findAllAktiv();

        assertTrue(aktive.stream().anyMatch(a -> a.getId().equals(aktiv.getId())));
        assertFalse(aktive.stream().anyMatch(a -> a.getId().equals(inaktiv.getId())));
    }

    @Test
    void updateAendertBestehendenDatensatz() {
        AktivitaetRepository repository = new AktivitaetRepository(TestDatabase.create());
        Aktivitaet aktivitaet = repository.save(neueAktivitaet());

        aktivitaet.setNotiz("Jaehrliches Herbstfest");
        repository.save(aktivitaet);

        var neuGeladen = repository.findById(aktivitaet.getId()).orElseThrow();
        assertEquals("Jaehrliches Herbstfest", neuGeladen.getNotiz());
    }

    private Aktivitaet neueAktivitaet() {
        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setName("Sankt Martin");
        return aktivitaet;
    }
}
