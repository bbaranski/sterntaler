package de.foerderverein.kasse.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;

public class MitgliedDialogController {

    private final AppContext ctx;
    private final ObservableList<Mitglied> mitglieder = FXCollections.observableArrayList();

    @FXML private TableView<Mitglied> mitgliederTable;
    @FXML private TableColumn<Mitglied, String> nameColumn;
    @FXML private TableColumn<Mitglied, String> statusColumn;
    @FXML private TextField vornameField;
    @FXML private TextField nachnameField;
    @FXML private TextField strasseField;
    @FXML private TextField plzField;
    @FXML private TextField ortField;
    @FXML private TextField emailField;
    @FXML private DatePicker eintrittsdatumPicker;
    @FXML private DatePicker austrittsdatumPicker;
    @FXML private ComboBox<MitgliedStatus> statusCombo;
    @FXML private TextArea notizField;
    @FXML private Label fehlerLabel;

    private Mitglied ausgewaehlt;

    public MitgliedDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getVollerName()));
        statusColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getStatus().name()));
        statusCombo.setItems(FXCollections.observableArrayList(MitgliedStatus.values()));
        mitgliederTable.setItems(mitglieder);

        mitgliederTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> zeigeInFormular(sel));

        lade();
        onNeu();
    }

    private void lade() {
        mitglieder.setAll(ctx.mitgliedService.findAll());
    }

    private void zeigeInFormular(Mitglied mitglied) {
        ausgewaehlt = mitglied;
        if (mitglied == null) {
            vornameField.clear();
            nachnameField.clear();
            strasseField.clear();
            plzField.clear();
            ortField.clear();
            emailField.clear();
            eintrittsdatumPicker.setValue(java.time.LocalDate.now());
            austrittsdatumPicker.setValue(null);
            statusCombo.setValue(MitgliedStatus.AKTIV);
            notizField.clear();
            return;
        }
        vornameField.setText(mitglied.getVorname());
        nachnameField.setText(mitglied.getNachname());
        strasseField.setText(mitglied.getStrasse());
        plzField.setText(mitglied.getPlz());
        ortField.setText(mitglied.getOrt());
        emailField.setText(mitglied.getEmail());
        eintrittsdatumPicker.setValue(mitglied.getEintrittsdatum());
        austrittsdatumPicker.setValue(mitglied.getAustrittsdatum());
        statusCombo.setValue(mitglied.getStatus());
        notizField.setText(mitglied.getNotiz());
    }

    @FXML
    private void onNeu() {
        mitgliederTable.getSelectionModel().clearSelection();
        zeigeInFormular(null);
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        try {
            Mitglied ziel = ausgewaehlt != null ? ausgewaehlt : new Mitglied();
            ziel.setVorname(vornameField.getText());
            ziel.setNachname(nachnameField.getText());
            ziel.setStrasse(leerZuNull(strasseField.getText()));
            ziel.setPlz(leerZuNull(plzField.getText()));
            ziel.setOrt(leerZuNull(ortField.getText()));
            ziel.setEmail(leerZuNull(emailField.getText()));
            ziel.setEintrittsdatum(eintrittsdatumPicker.getValue());
            ziel.setAustrittsdatum(austrittsdatumPicker.getValue());
            ziel.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : MitgliedStatus.AKTIV);
            ziel.setNotiz(leerZuNull(notizField.getText()));
            ctx.mitgliedService.save(ziel);
            lade();
            mitgliederTable.getSelectionModel().select(ziel);
        } catch (Exception e) {
            fehlerLabel.setText(e.getMessage());
        }
    }

    private String leerZuNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    @FXML
    private void onSchliessen() {
        ((Stage) vornameField.getScene().getWindow()).close();
    }
}
