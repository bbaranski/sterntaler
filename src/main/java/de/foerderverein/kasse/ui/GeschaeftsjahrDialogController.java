package de.foerderverein.kasse.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Geschaeftsjahr;

public class GeschaeftsjahrDialogController {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final AppContext ctx;
    private final ObservableList<Geschaeftsjahr> jahre = FXCollections.observableArrayList();

    @FXML private TableView<Geschaeftsjahr> jahreTable;
    @FXML private TableColumn<Geschaeftsjahr, String> bezeichnungColumn;
    @FXML private TableColumn<Geschaeftsjahr, String> zeitraumColumn;
    @FXML private TableColumn<Geschaeftsjahr, String> statusColumn;
    @FXML private TableColumn<Geschaeftsjahr, String> pruefungColumn;
    @FXML private TextField bezeichnungField;
    @FXML private DatePicker startDatumPicker;
    @FXML private DatePicker endDatumPicker;
    @FXML private TextField startsaldoField;
    @FXML private Button neuButton;
    @FXML private Button speichernButton;
    @FXML private DatePicker pruefungsdatumPicker;
    @FXML private TextField pruefungNameField;
    @FXML private Button abschliessenButton;
    @FXML private Button wiederOeffnenButton;
    @FXML private Label fehlerLabel;

    private Geschaeftsjahr ausgewaehlt;

    public GeschaeftsjahrDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        bezeichnungColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBezeichnung()));
        zeitraumColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().getStartDatum().format(DATUM_FORMAT) + " - " + cd.getValue().getEndDatum().format(DATUM_FORMAT)));
        statusColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(
                cd.getValue().isAbgeschlossen() ? "Abgeschlossen" : "Offen"));
        pruefungColumn.setCellValueFactory(cd -> {
            Geschaeftsjahr g = cd.getValue();
            if (g.getKassenpruefungDatum() == null) {
                return new ReadOnlyStringWrapper("");
            }
            return new ReadOnlyStringWrapper(g.getKassenpruefungDatum().format(DATUM_FORMAT)
                    + (g.getKassenpruefer() != null ? " (" + g.getKassenpruefer() + ")" : ""));
        });
        jahreTable.setItems(jahre);
        jahreTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            zeigeInFormular(sel);
            aktualisiereButtons(sel);
        });

        pruefungsdatumPicker.setValue(LocalDate.now());

        lade();
        onNeu();
    }

    private void lade() {
        jahre.setAll(ctx.geschaeftsjahrService.findAll());
    }

    private void zeigeInFormular(Geschaeftsjahr g) {
        ausgewaehlt = g;
        boolean gesperrt = g != null && g.isAbgeschlossen();
        if (g == null) {
            bezeichnungField.clear();
            startDatumPicker.setValue(null);
            endDatumPicker.setValue(null);
            startsaldoField.clear();
        } else {
            bezeichnungField.setText(g.getBezeichnung());
            startDatumPicker.setValue(g.getStartDatum());
            endDatumPicker.setValue(g.getEndDatum());
            startsaldoField.setText(g.getStartsaldoEuro().toPlainString().replace('.', ','));
        }
        bezeichnungField.setDisable(gesperrt);
        startDatumPicker.setDisable(gesperrt);
        endDatumPicker.setDisable(gesperrt);
        startsaldoField.setDisable(gesperrt);
        speichernButton.setDisable(gesperrt);
    }

    private void aktualisiereButtons(Geschaeftsjahr ausgewaehlt) {
        abschliessenButton.setDisable(ausgewaehlt == null || ausgewaehlt.isAbgeschlossen());
        wiederOeffnenButton.setDisable(ausgewaehlt == null || !ausgewaehlt.isAbgeschlossen());
    }

    @FXML
    private void onNeu() {
        jahreTable.getSelectionModel().clearSelection();
        zeigeInFormular(null);
        aktualisiereButtons(null);
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        try {
            BigDecimal startsaldo = new BigDecimal(startsaldoField.getText().trim().replace(',', '.'));
            LocalDate start = startDatumPicker.getValue();
            LocalDate end = endDatumPicker.getValue();
            String bezeichnung = bezeichnungField.getText();

            if (ausgewaehlt == null) {
                ctx.geschaeftsjahrService.erstellen(bezeichnung, start, end, startsaldo);
            } else {
                ctx.geschaeftsjahrService.aktualisiere(ausgewaehlt, bezeichnung, start, end, startsaldo);
            }
            lade();
            onNeu();
        } catch (NumberFormatException e) {
            fehlerLabel.setText("Startsaldo ungueltig, z. B. 1234,56");
        } catch (Exception e) {
            fehlerLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onAbschliessen() {
        fehlerLabel.setText("");
        Geschaeftsjahr ausgewaehlt = jahreTable.getSelectionModel().getSelectedItem();
        if (ausgewaehlt == null) {
            return;
        }
        if (pruefungsdatumPicker.getValue() == null) {
            fehlerLabel.setText("Bitte ein Pruefungsdatum waehlen");
            return;
        }
        ctx.geschaeftsjahrService.abschliessen(ausgewaehlt, pruefungsdatumPicker.getValue(), pruefungNameField.getText());
        lade();
        onNeu();
    }

    @FXML
    private void onWiederOeffnen() {
        Geschaeftsjahr ausgewaehlt = jahreTable.getSelectionModel().getSelectedItem();
        if (ausgewaehlt == null) {
            return;
        }
        ctx.geschaeftsjahrService.wiederOeffnen(ausgewaehlt);
        lade();
        onNeu();
    }

    @FXML
    private void onSchliessen() {
        ((Stage) bezeichnungField.getScene().getWindow()).close();
    }
}
