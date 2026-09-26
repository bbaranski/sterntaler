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
import de.foerderverein.kasse.domain.Bereich;
import de.foerderverein.kasse.domain.Kategorie;

public class KategorieDialogController {

    private final AppContext ctx;
    private final ObservableList<Kategorie> kategorien = FXCollections.observableArrayList();

    @FXML private TableView<Kategorie> kategorienTable;
    @FXML private TableColumn<Kategorie, String> nameColumn;
    @FXML private TableColumn<Kategorie, String> bereichColumn;
    @FXML private TextField nameField;
    @FXML private ComboBox<Bereich> bereichCombo;
    @FXML private CheckBox aktivCheck;
    @FXML private Label fehlerLabel;

    private Kategorie ausgewaehlt;

    public KategorieDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getName()));
        bereichColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBereich().name()));
        bereichCombo.setItems(FXCollections.observableArrayList(Bereich.values()));
        kategorienTable.setItems(kategorien);

        kategorienTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> zeigeInFormular(sel));

        lade();
        onNeu();
    }

    private void lade() {
        kategorien.setAll(ctx.kategorieService.findAll());
    }

    private void zeigeInFormular(Kategorie kategorie) {
        ausgewaehlt = kategorie;
        if (kategorie == null) {
            nameField.clear();
            bereichCombo.setValue(Bereich.IDEELLER_BEREICH);
            aktivCheck.setSelected(true);
            return;
        }
        nameField.setText(kategorie.getName());
        bereichCombo.setValue(kategorie.getBereich());
        aktivCheck.setSelected(kategorie.isAktiv());
    }

    @FXML
    private void onNeu() {
        kategorienTable.getSelectionModel().clearSelection();
        zeigeInFormular(null);
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        try {
            Kategorie ziel = ausgewaehlt != null ? ausgewaehlt : new Kategorie();
            ziel.setName(nameField.getText());
            ziel.setBereich(bereichCombo.getValue());
            ziel.setAktiv(aktivCheck.isSelected());
            ctx.kategorieService.save(ziel);
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
