package de.foerderverein.kasse.ui;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.config.AppConfig;

/** Abfrage der zu verwendenden Datenbankdatei beim Programmstart - laeuft bewusst als reines
 * Java-Fenster ohne FXML: {@link Views#loader} bzw. {@link Views#createController} setzen einen
 * bereits vorhandenen AppContext voraus, den dieser Dialog aber erst erzeugt (siehe zeigeDialog).
 * Nur beim Start relevant - ein Wechsel der Datenbank zur Laufzeit ist bewusst nicht vorgesehen,
 * dafuer wird die Anwendung neu gestartet. */
public final class DatenbankAuswahlDialog {

    private static final Preferences PREFS = Preferences.userNodeForPackage(DatenbankAuswahlDialog.class);
    private static final String LETZTER_PFAD_KEY = "letzterDatenbankpfad";

    private DatenbankAuswahlDialog() {
    }

    /** Zeigt den Auswahldialog modal an und liefert den erfolgreich geoeffneten AppContext, oder
     * ein leeres Optional, falls der Nutzer abgebrochen hat (die Anwendung soll sich in diesem
     * Fall beenden, siehe App.start). */
    public static Optional<AppContext> zeigeDialog() {
        AppContext[] ergebnis = new AppContext[1];

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Datenbank wählen");

        Label hinweis = new Label("Bitte Datenbankdatei wählen:");

        TextField pfadFeld = new TextField(PREFS.get(LETZTER_PFAD_KEY, AppConfig.databaseFile().toString()));
        pfadFeld.setPrefWidth(360);

        Label fehlerLabel = new Label();
        fehlerLabel.setWrapText(true);
        fehlerLabel.getStyleClass().add("fehler-label");

        Button oeffnenButton = new Button("Öffnen…");
        oeffnenButton.setOnAction(e -> {
            FileChooser chooser = neuerFileChooser(pfadFeld);
            File datei = chooser.showOpenDialog(stage);
            if (datei != null) {
                pfadFeld.setText(datei.getAbsolutePath());
                fehlerLabel.setText("");
            }
        });

        Button neueDatenbankButton = new Button("Neue Datenbank…");
        neueDatenbankButton.setOnAction(e -> {
            FileChooser chooser = neuerFileChooser(pfadFeld);
            chooser.setInitialFileName("kasse.db");
            File datei = chooser.showSaveDialog(stage);
            if (datei != null) {
                pfadFeld.setText(datei.getAbsolutePath());
                fehlerLabel.setText("");
            }
        });

        HBox auswahlZeile = new HBox(8, oeffnenButton, neueDatenbankButton);

        Button beendenButton = new Button("Beenden");
        beendenButton.setOnAction(e -> stage.close());

        Button startenButton = new Button("Starten");
        startenButton.setDefaultButton(true);
        startenButton.setOnAction(e -> {
            fehlerLabel.setText("");
            try {
                Path pfad = Path.of(pfadFeld.getText().trim());
                AppContext context = AppContext.create(pfad);
                PREFS.put(LETZTER_PFAD_KEY, pfad.toString());
                ergebnis[0] = context;
                stage.close();
            } catch (Exception ex) {
                fehlerLabel.setText("Datenbank konnte nicht geöffnet werden:\n" + ex.getMessage());
            }
        });

        HBox buttonZeile = new HBox(8, beendenButton, startenButton);
        buttonZeile.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(12, hinweis, pfadFeld, auswahlZeile, fehlerLabel, buttonZeile);
        root.setPadding(new Insets(16));

        stage.setScene(new Scene(root, 420, 240));
        stage.showAndWait();

        return Optional.ofNullable(ergebnis[0]);
    }

    private static FileChooser neuerFileChooser(TextField pfadFeld) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Datenbankdatei wählen");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SQLite-Datenbank", "*.db"),
                new FileChooser.ExtensionFilter("Alle Dateien", "*.*"));
        File aktuelleDatei = new File(pfadFeld.getText().trim());
        File verzeichnis = aktuelleDatei.getParentFile();
        chooser.setInitialDirectory(verzeichnis != null && verzeichnis.isDirectory()
                ? verzeichnis : AppConfig.appDir().toFile());
        return chooser;
    }
}
