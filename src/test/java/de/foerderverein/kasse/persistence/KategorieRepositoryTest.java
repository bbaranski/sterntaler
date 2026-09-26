package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Bereich;
import de.foerderverein.kasse.domain.Kategorie;

class KategorieRepositoryTest {

    @Test
    void starterKategorienSindNachMigrationVorhanden() {
        KategorieRepository repository = new KategorieRepository(TestDatabase.create());

        var alle = repository.findAllAktiv();

        assertTrue(alle.stream().anyMatch(k -> k.getName().equals("Mitgliedsbeiträge") && k.getBereich() == Bereich.IDEELLER_BEREICH));
        assertTrue(alle.stream().anyMatch(k -> k.getName().equals("Zinserträge") && k.getBereich() == Bereich.VERMOEGENSVERWALTUNG));
        assertFalse(alle.stream().anyMatch(k -> k.getName().equals("Spenden")));
    }

    @Test
    void findAllSortiertNachFachlicherBereichReihenfolge() {
        KategorieRepository repository = new KategorieRepository(TestDatabase.create());

        var alle = repository.findAll();

        var bereiche = alle.stream().map(Kategorie::getBereich).distinct().toList();
        var erwarteteReihenfolge = List.of(Bereich.IDEELLER_BEREICH, Bereich.VERMOEGENSVERWALTUNG,
                Bereich.ZWECKBETRIEB, Bereich.WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB);
        assertEquals(erwarteteReihenfolge, bereiche);
    }

    @Test
    void deaktivierteKategorieFehltInFindAllAktiv() {
        KategorieRepository repository = new KategorieRepository(TestDatabase.create());
        Kategorie neu = new Kategorie();
        neu.setName("Testkategorie");
        repository.save(neu);

        neu.setAktiv(false);
        repository.save(neu);

        assertFalse(repository.findAllAktiv().stream().anyMatch(k -> k.getName().equals("Testkategorie")));
    }
}
