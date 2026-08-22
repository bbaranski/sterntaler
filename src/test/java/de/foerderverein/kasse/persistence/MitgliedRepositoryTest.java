package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;

class MitgliedRepositoryTest {

    @Test
    void insertUndFindAll() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied mitglied = neuesMitglied();

        repository.save(mitglied);

        var alle = repository.findAll();
        assertEquals(1, alle.size());
        assertEquals("Erika", alle.get(0).getVorname());
    }

    @Test
    void findAllAktivFiltertInaktive() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied aktiv = repository.save(neuesMitglied());
        Mitglied inaktiv = neuesMitglied();
        inaktiv.setNachname("Ausgetreten");
        inaktiv.setStatus(MitgliedStatus.INAKTIV);
        repository.save(inaktiv);

        var aktive = repository.findAllAktiv();

        assertTrue(aktive.stream().anyMatch(m -> m.getId().equals(aktiv.getId())));
        assertFalse(aktive.stream().anyMatch(m -> m.getId().equals(inaktiv.getId())));
    }

    @Test
    void updateAendertBestehendenDatensatz() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied mitglied = repository.save(neuesMitglied());

        mitglied.setOrt("Musterstadt");
        repository.save(mitglied);

        var neuGeladen = repository.findById(mitglied.getId()).orElseThrow();
        assertEquals("Musterstadt", neuGeladen.getOrt());
    }

    @Test
    void emailWirdGespeichertUndGeladen() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied mitglied = neuesMitglied();
        mitglied.setEmail("erika@example.org");

        repository.save(mitglied);

        var neuGeladen = repository.findById(mitglied.getId()).orElseThrow();
        assertEquals("erika@example.org", neuGeladen.getEmail());
    }

    private Mitglied neuesMitglied() {
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        mitglied.setEintrittsdatum(LocalDate.of(2020, 1, 1));
        return mitglied;
    }
}
