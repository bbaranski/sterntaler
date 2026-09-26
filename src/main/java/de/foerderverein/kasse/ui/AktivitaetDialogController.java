package de.foerderverein.kasse.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Bereich;

/** Verwaltung der Veranstaltungen/Aktivitaeten des Wirtschaftlichen Geschaeftsbetriebs (z. B.
 * "Sankt Martin 2026"), denen Buchungen zugeordnet werden koennen - fuer die jaehrliche
 * "Ergaenzung zur Steuererklaerung". Leichter modaler Dialog analog zu KategorieDialogController,
 * keine eigene Hauptansicht. */
public class AktivitaetDialogController {

    private final AppContext ctx;
    private final ObservableList<Aktivitaet> aktivitaeten = FXCollections.observableArrayList();

    @FXML private TableView<Aktivitaet> aktivitaetenTable;
    @FXML private TableColumn<Aktivitaet, String> nameColumn;
    @FXML private TableColumn<Aktivitaet, String> bereichColumn;
    @FXML private TableColumn<Aktivitaet, String> aktivColumn;
    @FXML private TextField nameField;
    @FXML private TextField notizField;
    @FXML private ComboBox<Bereich> bereichCombo;
    @FXML private CheckBox aktivCheck;
    @FXML private Label fehlerLabel;

    private Aktivitaet ausgewaehlt;

    public AktivitaetDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        bereichColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBereich().name()));
        aktivColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().isAktiv() ? "Ja" : "Nein"));
        bereichCombo.setItems(FXCollections.observableArrayList(Bereich.values()));
        aktivitaetenTable.setItems(aktivitaeten);

        aktivitaetenTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> zeigeInFormular(sel));

        lade();
        onNeu();
    }

    private void lade() {
        aktivitaeten.setAll(ctx.aktivitaetService.findAll());
    }

    private void zeigeInFormular(Aktivitaet aktivitaet) {
        ausgewaehlt = aktivitaet;
        if (aktivitaet == null) {
            nameField.clear();
            notizField.clear();
            bereichCombo.setValue(Bereich.IDEELLER_BEREICH);
            aktivCheck.setSelected(true);
            return;
        }
        nameField.setText(aktivitaet.getName());
        notizField.setText(aktivitaet.getNotiz());
        bereichCombo.setValue(aktivitaet.getBereich());
        aktivCheck.setSelected(aktivitaet.isAktiv());
    }

    @FXML
    private void onNeu() {
        aktivitaetenTable.getSelectionModel().clearSelection();
        zeigeInFormular(null);
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        try {
            Aktivitaet ziel = ausgewaehlt != null ? ausgewaehlt : new Aktivitaet();
            ziel.setName(nameField.getText());
            ziel.setNotiz(notizField.getText() == null || notizField.getText().isBlank()
                    ? null : notizField.getText().trim());
            ziel.setBereich(bereichCombo.getValue());
            ziel.setAktiv(aktivCheck.isSelected());
            ctx.aktivitaetService.save(ziel);
            lade();
            onNeu();
        } catch (Exception e) {
            fehlerLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onSchliessen() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}
