package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Anrede;
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

    @Test
    void sepaUndAnredeFelderWerdenGespeichertUndGeladen() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied mitglied = neuesMitglied();
        mitglied.setAnrede(Anrede.FRAU);
        mitglied.setMandatsreferenz(1L);
        mitglied.setSammellastschrift("Sammler-2026-03");

        repository.save(mitglied);

        var neuGeladen = repository.findById(mitglied.getId()).orElseThrow();
        assertEquals(Anrede.FRAU, neuGeladen.getAnrede());
        assertEquals(1L, neuGeladen.getMandatsreferenz());
        assertEquals("Sammler-2026-03", neuGeladen.getSammellastschrift());
    }

    @Test
    void anredeDarfLeerBleiben() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied mitglied = repository.save(neuesMitglied());

        var neuGeladen = repository.findById(mitglied.getId()).orElseThrow();
        assertNull(neuGeladen.getAnrede());
    }

    private Mitglied neuesMitglied() {
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        mitglied.setEintrittsdatum("01.01.2020");
        return mitglied;
    }
}
