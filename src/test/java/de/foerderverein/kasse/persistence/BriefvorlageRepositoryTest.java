package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Briefvorlage;

class BriefvorlageRepositoryTest {

    private BriefvorlageRepository repository;

    @BeforeEach
    void setUp() {
        repository = new BriefvorlageRepository(TestDatabase.create());
    }

    @Test
    void speichernUndFindAll() {
        repository.speichern(neueVorlage("Vorstandssitzung"));

        var alle = repository.findAll();

        assertEquals(1, alle.size());
        assertEquals("Vorstandssitzung", alle.get(0).getName());
        assertEquals("Einladung", alle.get(0).getBetreff());
        assertEquals("vorlage.docx", alle.get(0).getSpeicherpfad());
        assertEquals("F. Thies", alle.get(0).getSignaturName());
    }

    @Test
    void speichernAktualisiertBestehendeVorlage() {
        Briefvorlage vorlage = repository.speichern(neueVorlage("Vorstandssitzung"));

        vorlage.setBetreff("Geänderter Betreff");
        Briefvorlage aktualisiert = repository.speichern(vorlage);

        assertEquals(vorlage.getId(), aktualisiert.getId());
        assertEquals(1, repository.findAll().size());
        assertEquals("Geänderter Betreff", repository.findAll().get(0).getBetreff());
    }

    @Test
    void loeschenEntferntVorlage() {
        Briefvorlage vorlage = repository.speichern(neueVorlage("Vorstandssitzung"));

        repository.loeschen(vorlage.getId());

        assertTrue(repository.findAll().isEmpty());
    }

    private Briefvorlage neueVorlage(String name) {
        Briefvorlage vorlage = new Briefvorlage();
        vorlage.setName(name);
        vorlage.setBetreff("Einladung");
        vorlage.setDateiname("vorlage.docx");
        vorlage.setSpeicherpfad("vorlage.docx");
        vorlage.setBrieftext("Tagesordnung folgt.");
        vorlage.setSignaturName("F. Thies");
        vorlage.setSignaturRolle("Schriftführerin");
        return vorlage;
    }
}
