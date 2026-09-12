package de.foerderverein.kasse.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
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

        String gespeicherteReihenfolge = PREFS.get(reihenfolgeKey(tabellenSchluessel), null);
        if (gespeicherteReihenfolge != null && !gespeicherteReihenfolge.isBlank()) {
            List<String> ids = List.of(gespeicherteReihenfolge.split(","));
            List<TableColumn<?, ?>> aktuelle = new ArrayList<>(table.getColumns());
            List<TableColumn<?, ?>> neu = new ArrayList<>();
            for (String id : ids) {
                for (TableColumn<?, ?> spalte : aktuelle) {
                    if (id.equals(spalteId(spalte)) && !neu.contains(spalte)) {
                        neu.add(spalte);
                    }
                }
            }
            // Spalten, die in der gespeicherten Reihenfolge fehlen (z. B. seither neu
            // hinzugekommene Felder), an das Ende anhaengen statt sie zu verlieren.
            for (TableColumn<?, ?> spalte : aktuelle) {
                if (!neu.contains(spalte)) {
                    neu.add(spalte);
                }
            }
            wendeReihenfolgeAn(table, neu);
        }

        table.getColumns().addListener((ListChangeListener<TableColumn<?, ?>>) change ->
                speichereReihenfolge(table, tabellenSchluessel));
    }

    /** Zeigt den Konfigurationsdialog. Bei "Übernehmen" wird die Sichtbarkeit sofort auf die
     * Tabelle angewendet und dauerhaft gespeichert; bei "Abbrechen" bleibt alles unveraendert.
     * Die Reihenfolge wird hier nicht angeboten - dafuer zieht man die Spaltenueberschrift
     * direkt in der Tabelle an die gewuenschte Stelle. */
    public static void zeigeDialog(TableView<?> table, String tabellenSchluessel, Window owner) {
        List<TableColumn<?, ?>> spalten = new ArrayList<>(table.getColumns());
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

        HBox buttonZeile = new HBox(8, alleButton, abbrechenButton, uebernehmenButton);
        buttonZeile.setAlignment(Pos.CENTER_RIGHT);

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
}
