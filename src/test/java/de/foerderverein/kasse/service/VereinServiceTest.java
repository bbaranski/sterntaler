package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Verein;
import de.foerderverein.kasse.persistence.TestDatabase;
import de.foerderverein.kasse.persistence.VereinRepository;

class VereinServiceTest {

    @Test
    void nameFehltWirdAbgelehnt() {
        VereinService service = new VereinService(new VereinRepository(TestDatabase.create()));
        Verein verein = new Verein();

        assertThrows(IllegalArgumentException.class, () -> service.save(verein));
    }

    @Test
    void ibanUndBicWerdenNormalisiert() {
        VereinService service = new VereinService(new VereinRepository(TestDatabase.create()));
        Verein verein = new Verein();
        verein.setName("Förderverein Musterschule e. V.");
        verein.setIban("de02 1203 0000 0000 2020 51");
        verein.setBic("byla dem1001");

        Verein gespeichert = service.save(verein);

        assertEquals("DE02120300000000202051", gespeichert.getIban());
        assertEquals("BYLADEM1001", gespeichert.getBic());
    }
}
