package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.AuditAktion;
import de.foerderverein.kasse.domain.AuditEintrag;
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
    void speichernProtokolliertVerlauf() {
        Database database = TestDatabase.create();
        MitgliedService service = new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database));
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        service.save(mitglied);

        mitglied.setOrt("Musterstadt");
        service.save(mitglied);

        var verlauf = service.findVerlauf(mitglied.getId());
        assertEquals(2, verlauf.size());
        // findVerlauf sortiert absteigend nach Zeitpunkt.
        assertEquals(AuditAktion.GEAENDERT, verlauf.get(0).getAktion());
        assertEquals(AuditAktion.ANGELEGT, verlauf.get(1).getAktion());
        assertEquals("Ort", verlauf.get(0).getFeld());
        assertEquals("", verlauf.get(0).getWertAlt());
        assertEquals("Musterstadt", verlauf.get(0).getWertNeu());
    }

    /** Mit dem Mitglied verschwindet auch seine Historie - sonst bliebe ein ueber die
     * Oberflaeche nicht mehr erreichbarer Rest mit personenbezogenen Daten zurueck. */
    @Test
    void loeschenEntferntAuchDenVerlauf() {
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

        assertTrue(service.findVerlauf(mitgliedId).isEmpty());
    }

    @Test
    void loeschenLaesstDenVerlaufAndererMitgliederUnberuehrt() {
        Database database = TestDatabase.create();
        MitgliedService service = new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database));
        Mitglied bleibt = new Mitglied();
        bleibt.setVorname("Max");
        bleibt.setNachname("Mustermann");
        service.save(bleibt);
        Mitglied geht = new Mitglied();
        geht.setVorname("Erika");
        geht.setNachname("Musterfrau");
        service.save(geht);

        service.loeschen(geht);

        assertEquals(1, service.findVerlauf(bleibt.getId()).size());
    }

    /** entity_id ist nur je Entitaetstyp eindeutig: eine Buchung kann dieselbe id tragen wie das
     * geloeschte Mitglied und darf ihre Historie behalten. */
    @Test
    void loeschenTrifftNurEintraegeDesMitglieds() {
        Database database = TestDatabase.create();
        AuditLogRepository auditLogRepository = new AuditLogRepository(database);
        MitgliedService service = new MitgliedService(new MitgliedRepository(database), auditLogRepository);
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        service.save(mitglied);

        AuditEintrag fremd = new AuditEintrag();
        fremd.setEntity("Buchung");
        fremd.setEntityId(mitglied.getId());
        fremd.setAktion(AuditAktion.ANGELEGT);
        fremd.setDetails("Buchung mit derselben id");
        auditLogRepository.insert(fremd);

        service.loeschen(mitglied);

        assertEquals(1, auditLogRepository.findByEntity("Buchung", mitglied.getId()).size());
    }
}
