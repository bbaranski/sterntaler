package de.foerderverein.kasse.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Bereich;
import de.foerderverein.kasse.domain.Kategorie;

class KategorieAuswahlTest {

    @Test
    void auswahlListeBeginntMitEintragZumEntfernen() {
        List<Kategorie> liste = KategorieAuswahl.alsAuswahlListe(List.of(
                kategorie(1L, "Geldspenden", Bereich.IDEELLER_BEREICH)));

        assertEquals("(ohne Kategorie)", liste.get(0).getName());
        // Ohne id - daraus leitet der Commit-Handler kategorieId = null ab.
        assertNull(liste.get(0).getId());
    }

    @Test
    void jedeBereichGruppeBekommtEineKopfzeile() {
        List<Kategorie> liste = KategorieAuswahl.alsAuswahlListe(List.of(
                kategorie(1L, "Geldspenden", Bereich.IDEELLER_BEREICH),
                kategorie(2L, "Mitgliedsbeiträge", Bereich.IDEELLER_BEREICH),
                kategorie(3L, "Zinserträge", Bereich.VERMOEGENSVERWALTUNG)));

        assertEquals(List.of("(ohne Kategorie)", "Ideeller Bereich", "Geldspenden", "Mitgliedsbeiträge",
                        "Vermögensverwaltung", "Zinserträge"),
                liste.stream().map(Kategorie::getName).toList());
    }

    /** Die Kopfzeilen sind nicht auswaehlbar - der Eintrag zum Entfernen dagegen schon, sonst
     * liesse sich die Zuordnung nicht loeschen. */
    @Test
    void nurDieBereichKopfzeilenSindNichtAuswaehlbar() {
        List<Kategorie> liste = KategorieAuswahl.alsAuswahlListe(List.of(
                kategorie(1L, "Geldspenden", Bereich.IDEELLER_BEREICH)));

        assertTrue(KategorieAuswahl.istBereichKopf(liste.get(1)));
        assertTrue(!KategorieAuswahl.istBereichKopf(liste.get(0)));
        assertTrue(!KategorieAuswahl.istBereichKopf(liste.get(2)));
    }

    @Test
    void leereKategorienlisteLiefertNurDenEntfernenEintrag() {
        List<Kategorie> liste = KategorieAuswahl.alsAuswahlListe(List.of());

        assertEquals(1, liste.size());
        assertEquals("(ohne Kategorie)", liste.get(0).getName());
    }

    private Kategorie kategorie(long id, String name, Bereich bereich) {
        Kategorie kategorie = new Kategorie();
        kategorie.setId(id);
        kategorie.setName(name);
        kategorie.setBereich(bereich);
        return kategorie;
    }
}
