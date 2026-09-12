package de.foerderverein.kasse.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/** Suche innerhalb einer TableView, konsistent fuer alle Ansichten (Buchungen, Mitglieder):
 * durchsucht pro Zeile einen von der aufrufenden Ansicht fest vorgegebenen Satz von Textwerten
 * (unabhaengig von der aktuellen Spaltensichtbarkeit, siehe {@link SpaltenKonfiguration} - das
 * Suchergebnis soll sich nicht aendern, nur weil eine Spalte gerade ausgeblendet ist), zeigt die
 * Trefferzahl an und erlaubt per Enter/Umschalt+Enter im Suchfeld oder per Weiter/Zurueck-Button
 * das Springen zwischen Treffern. */
final class TabellenSuche<S> {

    private final TableView<S> table;
    private final TextField suchFeld;
    private final Label trefferLabel;
    private final Function<S, List<String>> suchfelder;

    private List<Integer> treffer = List.of();
    private int aktuellerIndex = -1;

    TabellenSuche(TableView<S> table, TextField suchFeld, Button weiterButton, Button zurueckButton,
                  Label trefferLabel, Function<S, List<String>> suchfelder) {
        this.table = table;
        this.suchFeld = suchFeld;
        this.trefferLabel = trefferLabel;
        this.suchfelder = suchfelder;

        suchFeld.textProperty().addListener((obs, alt, neu) -> aktualisiereTreffer());
        suchFeld.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (event.isShiftDown()) {
                    zurueck();
                } else {
                    weiter();
                }
                event.consume();
            }
        });
        weiterButton.setOnAction(e -> weiter());
        zurueckButton.setOnAction(e -> zurueck());

        // Reagiert auf Neuladen/Aenderung der Tabellendaten (z. B. nach HBCI-Import oder
        // Speichern), damit die Treffer nicht auf veralteten Zeilenindizes stehen bleiben.
        table.getItems().addListener((ListChangeListener<S>) change -> aktualisiereTreffer());
    }

    private void aktualisiereTreffer() {
        String suchtext = suchFeld.getText();
        if (suchtext == null || suchtext.isBlank()) {
            treffer = List.of();
            aktuellerIndex = -1;
            setLabel("", false);
            return;
        }
        String suchtextKlein = suchtext.toLowerCase();
        List<S> zeilen = table.getItems();
        List<Integer> neueTreffer = new ArrayList<>();
        for (int i = 0; i < zeilen.size(); i++) {
            if (passtZeile(zeilen.get(i), suchtextKlein)) {
                neueTreffer.add(i);
            }
        }
        treffer = neueTreffer;
        aktuellerIndex = treffer.isEmpty() ? -1 : 0;
        zeigeAktuellenTreffer();
    }

    private boolean passtZeile(S zeile, String suchtextKlein) {
        for (String feld : suchfelder.apply(zeile)) {
            if (feld != null && feld.toLowerCase().contains(suchtextKlein)) {
                return true;
            }
        }
        return false;
    }

    private void weiter() {
        if (treffer.isEmpty()) {
            return;
        }
        aktuellerIndex = (aktuellerIndex + 1) % treffer.size();
        zeigeAktuellenTreffer();
    }

    private void zurueck() {
        if (treffer.isEmpty()) {
            return;
        }
        aktuellerIndex = (aktuellerIndex - 1 + treffer.size()) % treffer.size();
        zeigeAktuellenTreffer();
    }

    private void zeigeAktuellenTreffer() {
        if (treffer.isEmpty()) {
            setLabel("Keine Treffer", true);
            table.getSelectionModel().clearSelection();
            return;
        }
        setLabel("Treffer " + (aktuellerIndex + 1) + " von " + treffer.size(), false);
        int zeilenIndex = treffer.get(aktuellerIndex);
        table.getSelectionModel().clearAndSelect(zeilenIndex);
        table.scrollTo(zeilenIndex);
    }

    private void setLabel(String text, boolean fehler) {
        trefferLabel.setText(text);
        if (fehler) {
            if (!trefferLabel.getStyleClass().contains("fehler-label")) {
                trefferLabel.getStyleClass().add("fehler-label");
            }
        } else {
            trefferLabel.getStyleClass().remove("fehler-label");
        }
    }
}
