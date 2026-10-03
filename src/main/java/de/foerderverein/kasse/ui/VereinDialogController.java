package de.foerderverein.kasse.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Verein;

/** Verwaltung der Vereins-Stammdaten - anders als Kategorie/Aktivitaet/Konto ein reines Formular
 * ohne Liste/Tabelle, da es stets nur einen Verein-Datensatz gibt (siehe VereinRepository). */
public class VereinDialogController {

    private final AppContext ctx;

    @FXML private TextField nameField;
    @FXML private TextField strasseField;
    @FXML private TextField plzField;
    @FXML private TextField ortField;
    @FXML private TextField telefonField;
    @FXML private TextField emailField;
    @FXML private TextField homepageField;
    @FXML private TextField ibanField;
    @FXML private TextField bicField;
    @FXML private TextField banknameField;
    @FXML private Label fehlerLabel;

    private Verein verein;

    public VereinDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        ctx.vereinService.find().ifPresent(this::zeigeInFormular);
    }

    private void zeigeInFormular(Verein bestehend) {
        this.verein = bestehend;
        nameField.setText(bestehend.getName());
        strasseField.setText(bestehend.getStrasse());
        plzField.setText(bestehend.getPlz());
        ortField.setText(bestehend.getOrt());
        telefonField.setText(bestehend.getTelefon());
        emailField.setText(bestehend.getEmail());
        homepageField.setText(bestehend.getHomepage());
        ibanField.setText(bestehend.getIban());
        bicField.setText(bestehend.getBic());
        banknameField.setText(bestehend.getBankname());
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        try {
            Verein ziel = verein != null ? verein : new Verein();
            ziel.setName(nameField.getText());
            ziel.setStrasse(leerZuNull(strasseField.getText()));
            ziel.setPlz(leerZuNull(plzField.getText()));
            ziel.setOrt(leerZuNull(ortField.getText()));
            ziel.setTelefon(leerZuNull(telefonField.getText()));
            ziel.setEmail(leerZuNull(emailField.getText()));
            ziel.setHomepage(leerZuNull(homepageField.getText()));
            ziel.setIban(leerZuNull(ibanField.getText()));
            ziel.setBic(leerZuNull(bicField.getText()));
            ziel.setBankname(leerZuNull(banknameField.getText()));
            verein = ctx.vereinService.save(ziel);
        } catch (Exception e) {
            fehlerLabel.setText(e.getMessage());
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
