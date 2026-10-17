package de.foerderverein.kasse.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Aktivitaet;

class AktivitaetAuswahlTest {

    @Test
    void auswahlListeBeginntMitEintragZumEntfernen() {
        List<Aktivitaet> liste = AktivitaetAuswahl.alsAuswahlListe(List.of(aktivitaet(1L, "Sankt Martin 2026")));

        assertEquals("(ohne Aktivität)", liste.get(0).getName());
        // Ohne id - daraus leiten die Commit-Handler aktivitaetId = null ab.
        assertNull(liste.get(0).getId());
    }

    /** Die Tabellenspalte nutzt den eingebauten ComboBoxTableCell, der Eintraege ueber
     * toString() darstellt - der Eintrag muss sich also auch darueber richtig anzeigen. */
    @Test
    void eintragZumEntfernenStelltSichUeberToStringDar() {
        List<Aktivitaet> liste = AktivitaetAuswahl.alsAuswahlListe(List.of());

        assertEquals("(ohne Aktivität)", liste.get(0).toString());
    }

    @Test
    void reihenfolgeDerAktivitaetenBleibtErhalten() {
        List<Aktivitaet> liste = AktivitaetAuswahl.alsAuswahlListe(List.of(
                aktivitaet(1L, "Sankt Martin 2026"),
                aktivitaet(2L, "Schulfest 2026")));

        assertEquals(List.of("(ohne Aktivität)", "Sankt Martin 2026", "Schulfest 2026"),
                liste.stream().map(Aktivitaet::getName).toList());
    }

    @Test
    void leereAktivitaetenlisteLiefertNurDenEntfernenEintrag() {
        List<Aktivitaet> liste = AktivitaetAuswahl.alsAuswahlListe(List.of());

        assertEquals(1, liste.size());
        assertEquals("(ohne Aktivität)", liste.get(0).getName());
    }

    private Aktivitaet aktivitaet(long id, String name) {
        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setId(id);
        aktivitaet.setName(name);
        return aktivitaet;
    }
}
