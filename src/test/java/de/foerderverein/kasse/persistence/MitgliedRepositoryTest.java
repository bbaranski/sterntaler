package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

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
    void statusWirdPersistiert() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied aktiv = repository.save(neuesMitglied());
        Mitglied inaktiv = neuesMitglied();
        inaktiv.setNachname("Ausgetreten");
        inaktiv.setStatus(MitgliedStatus.INAKTIV);
        repository.save(inaktiv);

        var alle = repository.findAll();

        assertEquals(MitgliedStatus.AKTIV,
                alle.stream().filter(m -> m.getId().equals(aktiv.getId())).findFirst().orElseThrow().getStatus());
        assertEquals(MitgliedStatus.INAKTIV,
                alle.stream().filter(m -> m.getId().equals(inaktiv.getId())).findFirst().orElseThrow().getStatus());
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

    /** AUTOINCREMENT vergibt ids nie erneut - ohne Zuruecksetzen wuerde ein Import nach dem
     * Loeschen aller Mitglieder bei der zuletzt verwendeten id weiterzaehlen. */
    @Test
    void nachDemLoeschenAllerMitgliederBeginnenDieIdsWiederBeiEins() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        for (String nachname : new String[] {"Eins", "Zwei", "Drei"}) {
            Mitglied mitglied = neuesMitglied();
            mitglied.setNachname(nachname);
            repository.save(mitglied);
        }
        repository.findAll().forEach(m -> repository.loesche(m.getId()));
        assertTrue(repository.findAll().isEmpty());

        Mitglied neu = repository.save(neuesMitglied());

        assertEquals(1L, neu.getId());
    }

    /** Solange noch Mitglieder vorhanden sind, darf der Zaehler nicht angetastet werden: SQLite
     * wuerde dann auf max(id)+1 zurueckfallen und die id des geloeschten Mitglieds erneut
     * ausgeben. */
    @Test
    void bleibenMitgliederUebrigWirdKeineIdWiederverwendet() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied bleibt = repository.save(neuesMitglied());
        Mitglied hoeher = neuesMitglied();
        hoeher.setNachname("Hoeher");
        hoeher = repository.save(hoeher);
        long geloeschteId = hoeher.getId();
        repository.loesche(geloeschteId);

        Mitglied neu = repository.save(neuesMitglied());

        assertTrue(neu.getId() > geloeschteId,
                "id " + neu.getId() + " wurde wiederverwendet (geloescht war " + geloeschteId + ")");
        assertFalse(neu.getId().equals(bleibt.getId()));
    }

    /** In einem frischen Schema gibt es noch keinen sqlite_sequence-Eintrag - das Zuruecksetzen
     * muss dort ein wirkungsloser No-op sein. */
    @Test
    void ersteAnlageInLeeremSchemaBekommtIdEins() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());

        Mitglied neu = repository.save(neuesMitglied());

        assertEquals(1L, neu.getId());
    }

    /** insertAlle muss ganz oder gar nicht schreiben: scheitert eine Zeile mitten im Durchlauf,
     * darf keine der vorherigen stehenbleiben. */
    @Test
    void insertAlleSchreibtAlleOderKeines() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied gueltig = neuesMitglied();
        Mitglied ohneVorname = neuesMitglied();
        ohneVorname.setVorname(null);   // verletzt NOT NULL, umgeht die Service-Pruefung

        assertThrows(PersistenceException.class,
                () -> repository.insertAlle(List.of(gueltig, ohneVorname)));

        assertTrue(repository.findAll().isEmpty(),
                "Rollback hat nicht gegriffen, in der Tabelle stehen " + repository.findAll().size() + " Zeilen");
        assertNull(gueltig.getId(), "id darf nach dem Rollback nicht gesetzt bleiben");
    }

    @Test
    void insertAlleVergibtFortlaufendeIdsAbEins() {
        MitgliedRepository repository = new MitgliedRepository(TestDatabase.create());
        Mitglied mitReferenz = neuesMitglied();
        mitReferenz.setNachname("MitReferenz");
        mitReferenz.setMandatsreferenz(4711L);
        // Direkt danach eines ohne Referenz: das wiederverwendete PreparedStatement darf den
        // Wert der Vorzeile nicht stehenlassen.
        Mitglied ohneReferenz = neuesMitglied();
        ohneReferenz.setNachname("OhneReferenz");

        repository.insertAlle(List.of(mitReferenz, ohneReferenz));

        assertEquals(1L, mitReferenz.getId());
        assertEquals(2L, ohneReferenz.getId());
        assertEquals(4711L, repository.findById(1L).orElseThrow().getMandatsreferenz());
        assertNull(repository.findById(2L).orElseThrow().getMandatsreferenz());
    }

    private Mitglied neuesMitglied() {
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        mitglied.setEintrittsdatum("01.01.2020");
        return mitglied;
    }
}
