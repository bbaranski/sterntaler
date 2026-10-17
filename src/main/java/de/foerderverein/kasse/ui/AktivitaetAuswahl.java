package de.foerderverein.kasse.ui;

import java.util.ArrayList;
import java.util.List;

import de.foerderverein.kasse.domain.Aktivitaet;

/** Gemeinsame Aufbereitung der Aktivitaets-Dropdowns in Tabellenspalte und Buchungsdialog -
 * Gegenstueck zu KategorieAuswahl, hier aber ohne Gruppierung, da Aktivitaeten flach sind. */
final class AktivitaetAuswahl {

    private AktivitaetAuswahl() {
    }

    /** Auswaehlbarer Eintrag, der die Aktivitaets-Zuordnung wieder entfernt. Traegt bewusst keine
     * id - die Commit-Handler leiten daraus aktivitaetId = null ab. Darstellung laeuft ueber
     * Aktivitaet.toString(), weil die Tabellenspalte den eingebauten ComboBoxTableCell nutzt. */
    private static final class OhneAktivitaet extends Aktivitaet {
        OhneAktivitaet() {
            setName("(ohne Aktivität)");
        }
    }

    /** Stellt dem uebergebenen Bestand den Eintrag zum Entfernen voran; die Reihenfolge der
     * Aktivitaeten selbst bleibt unveraendert. */
    static List<Aktivitaet> alsAuswahlListe(List<Aktivitaet> aktivitaeten) {
        List<Aktivitaet> ergebnis = new ArrayList<>();
        ergebnis.add(new OhneAktivitaet());
        ergebnis.addAll(aktivitaeten);
        return ergebnis;
    }
}
