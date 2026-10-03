package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Verein;

class VereinRepositoryTest {

    @Test
    void findLiefertNichtsOhneGespeichertenVerein() {
        VereinRepository repository = new VereinRepository(TestDatabase.create());

        assertTrue(repository.find().isEmpty());
    }

    @Test
    void insertUndFind() {
        VereinRepository repository = new VereinRepository(TestDatabase.create());
        Verein verein = neuerVerein();

        repository.save(verein);

        var geladen = repository.find().orElseThrow();
        assertEquals("Förderverein Musterschule e. V.", geladen.getName());
        assertEquals("Musterstraße 1", geladen.getStrasse());
        assertEquals("12345", geladen.getPlz());
        assertEquals("Musterstadt", geladen.getOrt());
        assertEquals("0123 456789", geladen.getTelefon());
        assertEquals("verein@musterschule.de", geladen.getEmail());
        assertEquals("www.musterschule-verein.de", geladen.getHomepage());
        assertEquals("DE02120300000000202051", geladen.getIban());
        assertEquals("BYLADEM1001", geladen.getBic());
        assertEquals("Musterbank", geladen.getBankname());
    }

    @Test
    void updateAendertBestehendenDatensatzStattEinerZweitenZeile() {
        VereinRepository repository = new VereinRepository(TestDatabase.create());
        Verein verein = repository.save(neuerVerein());

        verein.setOrt("Neustadt");
        repository.save(verein);

        var geladen = repository.find().orElseThrow();
        assertEquals("Neustadt", geladen.getOrt());
        assertEquals(verein.getId(), geladen.getId());
    }

    private Verein neuerVerein() {
        Verein verein = new Verein();
        verein.setName("Förderverein Musterschule e. V.");
        verein.setStrasse("Musterstraße 1");
        verein.setPlz("12345");
        verein.setOrt("Musterstadt");
        verein.setTelefon("0123 456789");
        verein.setEmail("verein@musterschule.de");
        verein.setHomepage("www.musterschule-verein.de");
        verein.setIban("DE02120300000000202051");
        verein.setBic("BYLADEM1001");
        verein.setBankname("Musterbank");
        return verein;
    }
}
