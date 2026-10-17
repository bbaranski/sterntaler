package de.foerderverein.kasse.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;

import de.foerderverein.kasse.domain.Bereich;
import de.foerderverein.kasse.domain.Kategorie;

/** Gemeinsame Darstellung fuer Kategorie-Dropdowns (ComboBox im BuchungDialog, selbst verwaltete
 * ComboBox in der inline editierbaren Kategorie-Spalte der Buchungsuebersicht): fuegt vor jeder
 * Bereich-Gruppe einer bereits nach Bereich, Name sortierten Kategorienliste (siehe
 * BuchungenViewController.kategorienAktiv()) eine eigene, nicht auswaehlbare Kopfzeile mit dem
 * Bereich-Namen ein und stellt der Liste einen Eintrag zum Entfernen der Zuordnung voran. */
final class KategorieAuswahl {

    private KategorieAuswahl() {
    }

    /** Technisch eine Kategorie (damit sie in eine ComboBox&lt;Kategorie&gt; passt), aber nie echt,
     * nie gespeichert und nie auswaehlbar - die Maus wird durch das Deaktivieren der Zelle
     * blockiert (siehe zelle()), Tastatur-/programmatische Auswahl durch den Listener in
     * konfiguriere(...). */
    private static final class BereichKopf extends Kategorie {
        BereichKopf(Bereich bereich) {
            setBereich(bereich);
            setName(bereichText(bereich));
        }
    }

    /** Auswaehlbarer Eintrag, der die Kategorie-Zuordnung wieder entfernt. Traegt bewusst keine
     * id - der Commit-Handler leitet daraus kategorieId = null ab. Benannt wie der entsprechende
     * Sammelposten in Kassenbericht und Steuerreport, damit derselbe Zustand ueberall gleich
     * heisst. */
    private static final class OhneKategorie extends Kategorie {
        OhneKategorie() {
            setName("(ohne Kategorie)");
        }
    }

    /** Ob der Eintrag eine reine Bereich-Ueberschrift ist und damit nicht ausgewaehlt werden
     * darf - im Gegensatz zum Eintrag zum Entfernen der Zuordnung, der auswaehlbar sein muss. */
    static boolean istBereichKopf(Kategorie kategorie) {
        return kategorie instanceof BereichKopf;
    }

    /** Baut aus einer bereits nach Bereich, Name sortierten Kategorienliste die fertige
     * Dropdown-Liste: vorneweg der Eintrag zum Entfernen der Zuordnung, danach je Bereich-Gruppe
     * eine nicht auswaehlbare Kopfzeile. */
    static List<Kategorie> alsAuswahlListe(List<Kategorie> sortierteKategorien) {
        List<Kategorie> ergebnis = new ArrayList<>();
        ergebnis.add(new OhneKategorie());
        Bereich vorheriger = null;
        for (Kategorie kategorie : sortierteKategorien) {
            if (kategorie.getBereich() != vorheriger) {
                ergebnis.add(new BereichKopf(kategorie.getBereich()));
                vorheriger = kategorie.getBereich();
            }
            ergebnis.add(kategorie);
        }
        return ergebnis;
    }

    /** Richtet cellFactory, buttonCell und das Ueberspringen der Kopfzeilen bei der Auswahl ein -
     * erwartet, dass die Items der ComboBox bereits via alsAuswahlListe(...) aufbereitet sind. */
    static void konfiguriere(ComboBox<Kategorie> comboBox) {
        comboBox.setCellFactory(listView -> zelle());
        comboBox.setButtonCell(buttonZelle());
        comboBox.getSelectionModel().selectedIndexProperty().addListener((obs, alt, neu) -> {
            List<Kategorie> items = comboBox.getItems();
            int neuIndex = neu.intValue();
            if (neuIndex < 0 || neuIndex >= items.size() || !istBereichKopf(items.get(neuIndex))) {
                return;
            }
            // Ueberspringt die Kopfzeile in der Richtung, aus der die Auswahl kam (z. B. beim
            // Navigieren per Pfeiltaste) - faellt auf die vorherige Auswahl zurueck, falls am
            // Rand der Liste kein regulaerer Eintrag mehr folgt.
            int altIndex = alt.intValue();
            int schritt = neuIndex >= altIndex ? 1 : -1;
            int kandidat = neuIndex;
            while (kandidat >= 0 && kandidat < items.size() && istBereichKopf(items.get(kandidat))) {
                kandidat += schritt;
            }
            comboBox.getSelectionModel().select(kandidat >= 0 && kandidat < items.size() ? kandidat : altIndex);
        });
    }

    /** Fuer die aufgeklappte Dropdown-Liste - Kopfzeilen werden fett und deaktiviert dargestellt
     * (deaktiviert blockiert Mausklicks auf sie, siehe Klassenkommentar). Zellen werden von
     * JavaFX fuer unterschiedliche Items wiederverwendet, deshalb muss der Zustand bei jedem
     * updateItem-Aufruf explizit zurueckgesetzt werden. */
    private static ListCell<Kategorie> zelle() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Kategorie kategorie, boolean empty) {
                super.updateItem(kategorie, empty);
                getStyleClass().remove("kategorie-bereich-zeile");
                setDisable(false);
                if (empty || kategorie == null) {
                    setText(null);
                    return;
                }
                setText(kategorie.getName());
                if (istBereichKopf(kategorie)) {
                    getStyleClass().add("kategorie-bereich-zeile");
                    setDisable(true);
                }
            }
        };
    }

    /** Fuer die geschlossene ComboBox (ausgewaehlter Wert) - da eine BereichKopf-Zeile nie zum
     * ausgewaehlten Wert werden kann (siehe konfiguriere(...)), reicht hier eine schlichte
     * Textanzeige ohne Sonderbehandlung. */
    private static ListCell<Kategorie> buttonZelle() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Kategorie kategorie, boolean empty) {
                super.updateItem(kategorie, empty);
                setText(empty || kategorie == null ? null : kategorie.getName());
            }
        };
    }

    private static String bereichText(Bereich bereich) {
        return switch (bereich) {
            case IDEELLER_BEREICH -> "Ideeller Bereich";
            case VERMOEGENSVERWALTUNG -> "Vermögensverwaltung";
            case ZWECKBETRIEB -> "Zweckbetrieb";
            case WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB -> "Wirtschaftlicher Geschäftsbetrieb";
        };
    }
}
