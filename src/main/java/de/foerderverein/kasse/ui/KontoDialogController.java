package de.foerderverein.kasse.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Konto;

public class KontoDialogController {

    private final AppContext ctx;
    private final ObservableList<Konto> konten = FXCollections.observableArrayList();

    @FXML private ListView<Konto> kontenListe;
    @FXML private TextField bezeichnungField;
    @FXML private TextField ibanField;
    @FXML private TextField bicField;
    @FXML private TextField banknameField;
    @FXML private TextField blzField;
    @FXML private TextField userIdField;
    @FXML private TextField customerIdField;
    @FXML private Label fehlerLabel;

    private Konto ausgewaehlt;

    public KontoDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        kontenListe.setItems(konten);
        kontenListe.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> zeigeInFormular(sel));
        lade();
        if (!konten.isEmpty()) {
            kontenListe.getSelectionModel().selectFirst();
        } else {
            onNeu();
        }
    }

    private void lade() {
        konten.setAll(ctx.kontoService.findAll());
    }

    private void zeigeInFormular(Konto konto) {
        ausgewaehlt = konto;
        if (konto == null) {
            bezeichnungField.clear();
            ibanField.clear();
            bicField.clear();
            banknameField.clear();
            blzField.clear();
            userIdField.clear();
            customerIdField.clear();
            return;
        }
        bezeichnungField.setText(konto.getBezeichnung());
        ibanField.setText(konto.getIban());
        bicField.setText(konto.getBic());
        banknameField.setText(konto.getBankname());
        blzField.setText(konto.getHbciBlz());
        userIdField.setText(konto.getHbciUserId());
        customerIdField.setText(konto.getHbciCustomerId());
    }

    @FXML
    private void onNeu() {
        kontenListe.getSelectionModel().clearSelection();
        zeigeInFormular(null);
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        try {
            Konto ziel = ausgewaehlt != null ? ausgewaehlt : new Konto();
            ziel.setBezeichnung(bezeichnungField.getText());
            ziel.setIban(ibanField.getText());
            ziel.setBic(leerZuNull(bicField.getText()));
            ziel.setBankname(leerZuNull(banknameField.getText()));
            ziel.setHbciBlz(leerZuNull(blzField.getText()));
            ziel.setHbciUserId(leerZuNull(userIdField.getText()));
            ziel.setHbciCustomerId(leerZuNull(customerIdField.getText()));
            ctx.kontoService.save(ziel);
            lade();
            kontenListe.getSelectionModel().select(ziel);
        } catch (Exception e) {
            fehlerLabel.setText(e.getMessage());
        }
    }

    private String leerZuNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    @FXML
    private void onSchliessen() {
        ((Stage) bezeichnungField.getScene().getWindow()).close();
    }
}
