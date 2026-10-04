package de.foerderverein.kasse.ui;

import java.io.File;
import java.io.IOException;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Briefvorlage;
import de.foerderverein.kasse.service.SerienbriefService;

/** Verwaltung der Serienbrief-Vorlagen: je Anlass (z. B. "Vorstandssitzung") eine selbst in Word
 * gestaltete .docx-Datei mit Platzhaltern plus Vorschlagswerte fuer Betreff/Brieftext/Signatur.
 * Leichter modaler Dialog analog zu AktivitaetDialogController. */
public class BriefvorlageDialogController {

    private final AppContext ctx;
    private final ObservableList<Briefvorlage> vorlagen = FXCollections.observableArrayList();

    @FXML private TableView<Briefvorlage> vorlagenTable;
    @FXML private TableColumn<Briefvorlage, String> nameColumn;
    @FXML private TableColumn<Briefvorlage, String> betreffColumn;
    @FXML private TableColumn<Briefvorlage, String> dateinameColumn;
    @FXML private TextField nameField;
    @FXML private TextField betreffField;
    @FXML private TextArea brieftextArea;
    @FXML private TextField signaturNameField;
    @FXML private TextField signaturRolleField;
    @FXML private Label dateiLabel;
    @FXML private Label platzhalterLabel;
    @FXML private Label fehlerLabel;
    @FXML private Button loeschenButton;

    private Briefvorlage ausgewaehlt;
    private File neueDatei;

    public BriefvorlageDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        betreffColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBetreff()));
        dateinameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getDateiname()));
        vorlagenTable.setItems(vorlagen);
        vorlagenTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> zeigeInFormular(sel));

        platzhalterLabel.setText(String.join(", ",
                SerienbriefService.BEKANNTE_PLATZHALTER.stream().map(p -> "{{" + p + "}}").toList()));

        lade();
        onNeu();
    }

    private void lade() {
        vorlagen.setAll(ctx.briefvorlageService.findAll());
    }

    private void zeigeInFormular(Briefvorlage vorlage) {
        ausgewaehlt = vorlage;
        neueDatei = null;
        loeschenButton.setDisable(vorlage == null);
        if (vorlage == null) {
            nameField.clear();
            betreffField.clear();
            brieftextArea.clear();
            signaturNameField.clear();
            signaturRolleField.clear();
            dateiLabel.setText("(noch keine Datei gewählt)");
            return;
        }
        nameField.setText(vorlage.getName());
        betreffField.setText(vorlage.getBetreff());
        brieftextArea.setText(vorlage.getBrieftext());
        signaturNameField.setText(vorlage.getSignaturName());
        signaturRolleField.setText(vorlage.getSignaturRolle());
        dateiLabel.setText(vorlage.getDateiname());
    }

    @FXML
    private void onDateiWaehlen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Word-Vorlage auswählen");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word-Datei", "*.docx"));
        File datei = fileChooser.showOpenDialog(nameField.getScene().getWindow());
        if (datei == null) {
            return;
        }
        neueDatei = datei;
        dateiLabel.setText(datei.getName() + " (noch nicht gespeichert)");
        warneFallsKeinePlatzhalter(datei);
    }

    /** Reiner Hinweis, kein Fehler - die Datei laesst sich auch ohne Platzhalter speichern. */
    private void warneFallsKeinePlatzhalter(File datei) {
        try {
            if (!ctx.serienbriefService.enthaeltBekanntenPlatzhalter(datei)) {
                Alert hinweis = new Alert(Alert.AlertType.WARNING,
                        "In der Datei wurde kein bekannter Platzhalter (z. B. {{Vorname}}) gefunden. "
                                + "Ist das die richtige Datei?");
                hinweis.setHeaderText("Keine Platzhalter gefunden");
                hinweis.showAndWait();
            }
        } catch (IOException e) {
            fehlerLabel.setText("Datei konnte nicht gelesen werden: " + e.getMessage());
        }
    }

    @FXML
    private void onNeu() {
        vorlagenTable.getSelectionModel().clearSelection();
        zeigeInFormular(null);
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        try {
            Briefvorlage ziel = ausgewaehlt != null ? ausgewaehlt : new Briefvorlage();
            ziel.setName(nameField.getText());
            ziel.setBetreff(betreffField.getText());
            ziel.setBrieftext(brieftextArea.getText());
            ziel.setSignaturName(leerZuNull(signaturNameField.getText()));
            ziel.setSignaturRolle(leerZuNull(signaturRolleField.getText()));
            ctx.briefvorlageService.speichern(ziel, neueDatei);
            lade();
            onNeu();
        } catch (IOException | RuntimeException e) {
            fehlerLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onLoeschen() {
        if (ausgewaehlt == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Vorlage \"" + ausgewaehlt.getName() + "\" entfernen? Die hinterlegte Word-Datei wird dabei gelöscht.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Vorlage löschen");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            ctx.briefvorlageService.entfernen(ausgewaehlt);
            lade();
            onNeu();
        }
    }

    @FXML
    private void onSchliessen() {
        ((Stage) nameField.getScene().getWindow()).close();
    }

    private String leerZuNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
