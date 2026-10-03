package de.foerderverein.kasse.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/** Generischer "Spalten konfigurieren"-Dialog fuer eine beliebige TableView: eine Checkbox je
 * Spalte (Beschriftung = Spaltentitel, Anfangszustand = aktuelle Sichtbarkeit), Aenderungen
 * werden sofort angewendet und dauerhaft ueber java.util.prefs.Preferences gespeichert, damit
 * sie einen Neustart der Anwendung ueberstehen. Zusaetzlich wird die Spaltenreihenfolge
 * beobachtet: JavaFX aendert beim Ziehen einer Spaltenueberschrift (Drag&Drop) automatisch die
 * Reihenfolge in {@code TableView.getColumns()} - diese Aenderung wird hier erkannt und ebenso
 * dauerhaft gespeichert.
 *
 * <p>Setzt voraus, dass jede TableColumn ein stabiles fx:id besitzt - der FXMLLoader setzt
 * dieses automatisch auch als {@code TableColumnBase.getId()}, das hier als Schluessel fuer die
 * Preferences dient (Spaltentitel allein waere keine stabile Kennung, falls er sich mal
 * aendert). */
public final class SpaltenKonfiguration {

    private static final Preferences PREFS = Preferences.userNodeForPackage(SpaltenKonfiguration.class);

    private SpaltenKonfiguration() {
    }

    /** Wendet die zuletzt gespeicherte Sichtbarkeit und Reihenfolge an und merkt sich ab sofort
     * jede weitere Umsortierung (z. B. per Drag&Drop) automatisch. Vor der ersten Anzeige der
     * Tabelle aufzurufen. */
    public static void wiederherstellen(TableView<?> table, String tabellenSchluessel) {
        for (TableColumn<?, ?> column : table.getColumns()) {
            String gespeichert = PREFS.get(sichtbarkeitKey(tabellenSchluessel, column), null);
            if (gespeichert != null) {
                column.setVisible(Boolean.parseBoolean(gespeichert));
            }
        }

        // Muss vor dem Anwenden einer gespeicherten (ggf. per Drag&Drop veraenderten) Reihenfolge
        // erfolgen, damit hier wirklich die aus dem FXML geladene, pristine Reihenfolge einmalig
        // gesichert wird - sie ist die Grundlage fuer die Standardansicht im Spalten-Dialog und
        // fuer den Zuruecksetzen-Button dort.
        sichereStandardReihenfolgeFallsFehlt(table, tabellenSchluessel);

        String gespeicherteReihenfolge = PREFS.get(reihenfolgeKey(tabellenSchluessel), null);
        if (gespeicherteReihenfolge != null && !gespeicherteReihenfolge.isBlank()) {
            wendeReihenfolgeAn(table, geordneteSpalten(table, List.of(gespeicherteReihenfolge.split(","))));
        }

        table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>) change ->
                speichereReihenfolge(table, tabellenSchluessel));
    }

    private static void sichereStandardReihenfolgeFallsFehlt(TableView<?> table, String tabellenSchluessel) {
        if (PREFS.get(standardReihenfolgeKey(tabellenSchluessel), null) == null) {
            String standard = table.getColumns().stream()
                    .map(SpaltenKonfiguration::spalteId)
                    .collect(Collectors.joining(","));
            PREFS.put(standardReihenfolgeKey(tabellenSchluessel), standard);
        }
    }

    /** Ordnet die aktuellen Spalten der Tabelle gemaess der uebergebenen Id-Liste an. Spalten, die
     * in der Liste fehlen (z. B. seither neu hinzugekommene Felder), werden an der Position
     * eingefuegt, die ihrer urspruenglichen (FXML-)Reihenfolge entspricht - direkt hinter der
     * zuletzt eingeordneten vorangehenden Spalte - statt pauschal ans Ende angehaengt zu werden,
     * damit eine neue Spalte an ihrer im FXML vorgesehenen Stelle auftaucht, auch wenn der Nutzer
     * bereits eine eigene Reihenfolge gespeichert hat. Stale Ids (seither entfernte Spalten)
     * werden stillschweigend ignoriert. */
    private static List<TableColumn<?, ?>> geordneteSpalten(TableView<?> table, List<String> ids) {
        List<TableColumn<?, ?>> aktuelle = new ArrayList<>(table.getColumns());
        List<TableColumn<?, ?>> ergebnis = new ArrayList<>();
        for (String id : ids) {
            for (TableColumn<?, ?> spalte : aktuelle) {
                if (id.equals(spalteId(spalte)) && !ergebnis.contains(spalte)) {
                    ergebnis.add(spalte);
                }
            }
        }
        int einfuegePosition = 0;
        for (TableColumn<?, ?> spalte : aktuelle) {
            if (ergebnis.contains(spalte)) {
                einfuegePosition = ergebnis.indexOf(spalte) + 1;
            } else {
                ergebnis.add(einfuegePosition, spalte);
                einfuegePosition++;
            }
        }
        return ergebnis;
    }

    /** Die urspruengliche, aus dem FXML geladene Spaltenreihenfolge - unabhaengig davon, ob der
     * Nutzer sie inzwischen per Drag&Drop veraendert hat. */
    private static List<TableColumn<?, ?>> standardReihenfolge(TableView<?> table, String tabellenSchluessel) {
        String standard = PREFS.get(standardReihenfolgeKey(tabellenSchluessel), null);
        if (standard == null || standard.isBlank()) {
            return new ArrayList<>(table.getColumns());
        }
        return geordneteSpalten(table, List.of(standard.split(",")));
    }

    /** Zeigt den Konfigurationsdialog. Die Checkboxen werden in der Standardreihenfolge (aus dem
     * FXML) angezeigt, unabhaengig von einer eventuell per Drag&Drop veraenderten aktuellen
     * Reihenfolge der Tabelle. Bei "Übernehmen" wird die Sichtbarkeit sofort auf die Tabelle
     * angewendet und dauerhaft gespeichert; bei "Abbrechen" bleibt die Sichtbarkeit unveraendert.
     * Die Reihenfolge selbst wird hier nicht direkt angeboten - dafuer zieht man die
     * Spaltenueberschrift in der Tabelle an die gewuenschte Stelle, oder setzt sie ueber den
     * Zuruecksetzen-Button unten wieder auf die Standardreihenfolge zurueck. */
    public static void zeigeDialog(TableView<?> table, String tabellenSchluessel, Window owner) {
        List<TableColumn<?, ?>> spalten = standardReihenfolge(table, tabellenSchluessel);
        List<CheckBox> checkboxen = new ArrayList<>();
        VBox liste = new VBox(6);
        liste.setPadding(new Insets(4));
        for (TableColumn<?, ?> spalte : spalten) {
            CheckBox checkBox = new CheckBox(spalte.getText());
            checkBox.setSelected(spalte.isVisible());
            checkboxen.add(checkBox);
            liste.getChildren().add(checkBox);
        }

        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Spalten konfigurieren");

        Button alleButton = new Button("Alle anzeigen");
        alleButton.setOnAction(e -> checkboxen.forEach(cb -> cb.setSelected(true)));

        // Wirkt sofort auf die Tabelle (wie ein manuelles Ziehen der Spaltenueberschrift) - der
        // in wiederherstellen() registrierte Listener speichert die neue Reihenfolge automatisch,
        // eine gesonderte Behandlung ist hier nicht noetig.
        Button reihenfolgeZuruecksetzenButton = new Button("Reihenfolge zurücksetzen");
        reihenfolgeZuruecksetzenButton.setOnAction(e -> wendeReihenfolgeAn(table, standardReihenfolge(table, tabellenSchluessel)));

        Button abbrechenButton = new Button("Abbrechen");
        abbrechenButton.setOnAction(e -> stage.close());

        Button uebernehmenButton = new Button("Übernehmen");
        uebernehmenButton.setDefaultButton(true);
        uebernehmenButton.setOnAction(e -> {
            for (int i = 0; i < spalten.size(); i++) {
                boolean sichtbar = checkboxen.get(i).isSelected();
                spalten.get(i).setVisible(sichtbar);
                PREFS.put(sichtbarkeitKey(tabellenSchluessel, spalten.get(i)), Boolean.toString(sichtbar));
            }
            stage.close();
        });

        for (Button button : List.of(alleButton, reihenfolgeZuruecksetzenButton, abbrechenButton, uebernehmenButton)) {
            button.setMaxWidth(Double.MAX_VALUE);
        }
        VBox buttonZeile = new VBox(8, alleButton, reihenfolgeZuruecksetzenButton, abbrechenButton, uebernehmenButton);

        ScrollPane scrollPane = new ScrollPane(liste);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(320);

        VBox root = new VBox(12, scrollPane, buttonZeile);
        root.setPadding(new Insets(16));

        stage.setScene(new Scene(root, 300, 420));
        stage.showAndWait();
    }

    /** Kapselt den unvermeidlichen unchecked Cast: die uebergebenen Spalten stammen selbst aus
     * table.getColumns() (nur umsortiert), der Cast auf den passenden Typparameter der Tabelle
     * ist also zur Laufzeit immer sicher - Java kann das bei einer TableView&lt;?&gt; nur nicht
     * selbst pruefen. */
    @SuppressWarnings("unchecked")
    private static <S> void wendeReihenfolgeAn(TableView<S> table, List<TableColumn<?, ?>> neueReihenfolge) {
        List<TableColumn<S, ?>> typisiert = new ArrayList<>();
        for (TableColumn<?, ?> spalte : neueReihenfolge) {
            typisiert.add((TableColumn<S, ?>) spalte);
        }
        table.getColumns().setAll(typisiert);
    }

    private static void speichereReihenfolge(TableView<?> table, String tabellenSchluessel) {
        String reihenfolge = table.getColumns().stream()
                .map(SpaltenKonfiguration::spalteId)
                .collect(Collectors.joining(","));
        PREFS.put(reihenfolgeKey(tabellenSchluessel), reihenfolge);
    }

    private static String spalteId(TableColumn<?, ?> column) {
        return column.getId() != null && !column.getId().isBlank() ? column.getId() : column.getText();
    }

    private static String sichtbarkeitKey(String tabellenSchluessel, TableColumn<?, ?> column) {
        return "spalten." + tabellenSchluessel + "." + spalteId(column);
    }

    private static String reihenfolgeKey(String tabellenSchluessel) {
        return "spaltenreihenfolge." + tabellenSchluessel;
    }

    private static String standardReihenfolgeKey(String tabellenSchluessel) {
        return "spaltenstandardreihenfolge." + tabellenSchluessel;
    }
}
