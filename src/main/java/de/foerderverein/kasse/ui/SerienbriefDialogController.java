package de.foerderverein.kasse.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Briefvorlage;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.service.SerienbriefService.SerienbriefMailEntwurf;

/** Erzeugt aus einer Serienbrief-Vorlage und den im Mitglieder-Tab ausgewaehlten Empfaengern
 * entweder eine zusammengefuehrte Word-Datei zum Drucken oder eine einzelne Sammel-E-Mail mit
 * allen Empfaengern im BCC. */
public class SerienbriefDialogController {

    private final AppContext ctx;
    private final ObservableList<Briefvorlage> vorlagen = FXCollections.observableArrayList();

    @FXML private Label empfaengerLabel;
    @FXML private ComboBox<Briefvorlage> vorlageCombo;
    @FXML private TextArea brieftextArea;
    @FXML private TextField signaturNameField;
    @FXML private TextField signaturRolleField;
    @FXML private RadioButton druckOption;
    @FXML private Label fehlerLabel;

    private List<Mitglied> empfaenger = List.of();

    public SerienbriefDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        vorlageCombo.setItems(vorlagen);
        vorlageCombo.valueProperty().addListener((obs, alt, neu) -> zeigeVorlage(neu));
        vorlagen.setAll(ctx.briefvorlageService.findAll());
        if (!vorlagen.isEmpty()) {
            vorlageCombo.setValue(vorlagen.get(0));
        } else {
            fehlerLabel.setText("Es ist noch keine Vorlage angelegt - bitte zuerst unter "
                    + "\"Einstellungen → Serienbriefvorlagen…\" eine anlegen.");
        }
    }

    public void setEmpfaenger(List<Mitglied> empfaenger) {
        this.empfaenger = List.copyOf(empfaenger);
        empfaengerLabel.setText(this.empfaenger.size() + " ausgewählte Mitglieder");
    }

    private void zeigeVorlage(Briefvorlage vorlage) {
        if (vorlage == null) {
            return;
        }
        brieftextArea.setText(vorlage.getBrieftext());
        signaturNameField.setText(vorlage.getSignaturName());
        signaturRolleField.setText(vorlage.getSignaturRolle());
    }

    @FXML
    private void onErzeugen() {
        fehlerLabel.setText("");
        Briefvorlage vorlage = vorlageCombo.getValue();
        if (vorlage == null) {
            fehlerLabel.setText("Bitte zuerst eine Vorlage auswählen.");
            return;
        }
        if (empfaenger.isEmpty()) {
            fehlerLabel.setText("Es sind keine Empfänger ausgewählt.");
            return;
        }
        if (druckOption.isSelected()) {
            erzeugeDruckdatei(vorlage);
        } else {
            oeffneSammelMail(vorlage);
        }
    }

    private void erzeugeDruckdatei(Briefvorlage vorlage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Serienbrief speichern");
        fileChooser.setInitialFileName(vorlage.getName().replaceAll("[\\\\/:*?\"<>|]", "_") + ".docx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word-Datei", "*.docx"));
        File datei = fileChooser.showSaveDialog(brieftextArea.getScene().getWindow());
        if (datei == null) {
            return;
        }
        try {
            Path erzeugt = ctx.serienbriefService.erstelleSammeldruckDocx(vorlage, empfaenger, brieftextArea.getText(),
                    signaturNameField.getText(), signaturRolleField.getText(), datei.toPath());
            oeffneDatei(erzeugt.toFile());
            schliessen();
        } catch (IOException | RuntimeException e) {
            fehlerLabel.setText("Serienbrief konnte nicht erzeugt werden: " + e.getMessage());
        }
    }

    private void oeffneSammelMail(Briefvorlage vorlage) {
        SerienbriefMailEntwurf entwurf = ctx.serienbriefService.erstelleMailEntwurf(empfaenger, vorlage.getBetreff(),
                brieftextArea.getText(), signaturNameField.getText(), signaturRolleField.getText());
        if (entwurf.bccAdressen().isEmpty()) {
            fehlerLabel.setText("Keines der ausgewählten Mitglieder hat eine E-Mail-Adresse hinterlegt.");
            return;
        }
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.MAIL)) {
            fehlerLabel.setText("Es ist kein Standard-Mailprogramm verfügbar.");
            return;
        }
        try {
            Desktop.getDesktop().mail(mailtoUri(entwurf));
            meldeMitgliederOhneEmail(entwurf);
            schliessen();
        } catch (IOException | URISyntaxException e) {
            fehlerLabel.setText("Mail-Fenster konnte nicht geöffnet werden: " + e.getMessage());
        }
    }

    /** Baut die mailto-URI mit allen Empfaengern im BCC. Die Bestandteile werden einzeln
     * prozent-kodiert (URLEncoder kodiert Leerzeichen als "+", in einer URI wird aber "%20"
     * erwartet), der Gesamtstring ist danach bereits gueltig kodiert und wird direkt als URI
     * geparst. */
    private URI mailtoUri(SerienbriefMailEntwurf entwurf) throws URISyntaxException {
        String vereinsEmail = ctx.vereinService.find().map(v -> v.getEmail()).orElse(null);
        StringBuilder uri = new StringBuilder("mailto:");
        if (vereinsEmail != null && !vereinsEmail.isBlank()) {
            uri.append(kodiere(vereinsEmail));
        }
        uri.append("?bcc=").append(kodiere(String.join(",", entwurf.bccAdressen())));
        uri.append("&subject=").append(kodiere(entwurf.betreff()));
        uri.append("&body=").append(kodiere(entwurf.text()));
        return new URI(uri.toString());
    }

    private String kodiere(String wert) {
        return URLEncoder.encode(wert, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void meldeMitgliederOhneEmail(SerienbriefMailEntwurf entwurf) {
        if (entwurf.ohneEmail().isEmpty()) {
            return;
        }
        String namen = String.join(", ", entwurf.ohneEmail().stream().map(Mitglied::getVollerName).toList());
        Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                entwurf.ohneEmail().size() + " Mitglieder haben keine E-Mail-Adresse und wurden nicht angeschrieben:\n"
                        + namen);
        hinweis.setHeaderText("Mitglieder ohne E-Mail-Adresse");
        hinweis.showAndWait();
    }

    /** Oeffnet die erzeugte Datei im Standardprogramm (Word/Pages) - schlaegt das fehl, ist das
     * kein Fehler, die Datei liegt trotzdem am Zielort. */
    private void oeffneDatei(File datei) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(datei);
            }
        } catch (Exception e) {
            // Bewusst ignoriert, siehe Methodenkommentar.
        }
    }

    @FXML
    private void onAbbrechen() {
        schliessen();
    }

    private void schliessen() {
        ((Stage) brieftextArea.getScene().getWindow()).close();
    }
}
