package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;
import de.foerderverein.kasse.persistence.MitgliedRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class MitgliedServiceTest {

    @Test
    void nachnameFehltWirdAbgelehnt() {
        MitgliedService service = new MitgliedService(new MitgliedRepository(TestDatabase.create()));
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");

        assertThrows(IllegalArgumentException.class, () -> service.save(mitglied));
    }

    @Test
    void austretenSetztStatusInaktiv() {
        MitgliedService service = new MitgliedService(new MitgliedRepository(TestDatabase.create()));
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        service.save(mitglied);

        service.austreten(mitglied);

        assertEquals(MitgliedStatus.INAKTIV, mitglied.getStatus());
    }
}
