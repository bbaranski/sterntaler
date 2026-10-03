package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.AuditAktion;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.MitgliedRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class MitgliedServiceTest {

    @Test
    void nachnameFehltWirdAbgelehnt() {
        Database database = TestDatabase.create();
        MitgliedService service = new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database));
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");

        assertThrows(IllegalArgumentException.class, () -> service.save(mitglied));
    }

    @Test
    void austretenSetztStatusInaktiv() {
        Database database = TestDatabase.create();
        MitgliedService service = new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database));
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        service.save(mitglied);

        service.austreten(mitglied);

        assertEquals(MitgliedStatus.INAKTIV, mitglied.getStatus());
    }

    @Test
    void loeschenEntferntMitgliedOhneBuchungen() {
        Database database = TestDatabase.create();
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        MitgliedService service = new MitgliedService(mitgliedRepository, new AuditLogRepository(database));
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        service.save(mitglied);

        service.loeschen(mitglied);

        assertTrue(mitgliedRepository.findById(mitglied.getId()).isEmpty());
    }

    @Test
    void speichernUndLoeschenProtokollierenVerlauf() {
        Database database = TestDatabase.create();
        MitgliedService service = new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database));
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        service.save(mitglied);

        mitglied.setOrt("Musterstadt");
        service.save(mitglied);

        long mitgliedId = mitglied.getId();
        service.loeschen(mitglied);

        var verlauf = service.findVerlauf(mitgliedId);
        assertEquals(3, verlauf.size());
        // findVerlauf sortiert absteigend nach Zeitpunkt - zuletzt geschieht das Loeschen.
        assertEquals(AuditAktion.GELOESCHT, verlauf.get(0).getAktion());
        assertEquals(AuditAktion.GEAENDERT, verlauf.get(1).getAktion());
        assertEquals(AuditAktion.ANGELEGT, verlauf.get(2).getAktion());
        assertEquals("Ort", verlauf.get(1).getFeld());
        assertEquals("", verlauf.get(1).getWertAlt());
        assertEquals("Musterstadt", verlauf.get(1).getWertNeu());
    }
}
