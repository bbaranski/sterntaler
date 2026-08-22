package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.AuditAktion;
import de.foerderverein.kasse.domain.AuditEintrag;

class AuditLogRepositoryTest {

    @Test
    void insertUndFindByEntity() {
        AuditLogRepository repository = new AuditLogRepository(TestDatabase.create());

        repository.insert(eintrag(1, AuditAktion.ANGELEGT, "tester"));
        repository.insert(eintrag(1, AuditAktion.GEAENDERT, "tester"));
        repository.insert(eintrag(2, AuditAktion.ANGELEGT, "anderer"));

        var verlauf = repository.findByEntity("Buchung", 1);

        assertEquals(2, verlauf.size());
        assertTrue(verlauf.stream().allMatch(e -> e.getEntityId() == 1));
    }

    @Test
    void findByEntityLiefertNichtsFuerUnbekannteEntity() {
        AuditLogRepository repository = new AuditLogRepository(TestDatabase.create());
        repository.insert(eintrag(1, AuditAktion.ANGELEGT, "tester"));

        assertEquals(0, repository.findByEntity("Buchung", 999).size());
    }

    private AuditEintrag eintrag(long entityId, AuditAktion aktion, String benutzer) {
        AuditEintrag eintrag = new AuditEintrag();
        eintrag.setEntity("Buchung");
        eintrag.setEntityId(entityId);
        eintrag.setAktion(aktion);
        eintrag.setBenutzer(benutzer);
        eintrag.setDetails("Testdetails");
        return eintrag;
    }
}
