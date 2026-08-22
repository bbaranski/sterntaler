package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.KategorieTyp;

class KategorieRepositoryTest {

    @Test
    void starterKategorienSindNachMigrationVorhanden() {
        KategorieRepository repository = new KategorieRepository(TestDatabase.create());

        var alle = repository.findAllAktiv();

        assertTrue(alle.stream().anyMatch(k -> k.getName().equals("Spenden") && k.getTyp() == KategorieTyp.EINNAHME));
        assertTrue(alle.stream().anyMatch(k -> k.getName().equals("Bankgebuehren") && k.getTyp() == KategorieTyp.AUSGABE));
    }

    @Test
    void deaktivierteKategorieFehltInFindAllAktiv() {
        KategorieRepository repository = new KategorieRepository(TestDatabase.create());
        Kategorie neu = new Kategorie();
        neu.setName("Testkategorie");
        neu.setTyp(KategorieTyp.AUSGABE);
        repository.save(neu);

        neu.setAktiv(false);
        repository.save(neu);

        assertFalse(repository.findAllAktiv().stream().anyMatch(k -> k.getName().equals("Testkategorie")));
    }
}
