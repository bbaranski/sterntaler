package de.foerderverein.kasse.ui;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.hbci.HbciException;
import de.foerderverein.kasse.hbci.HbciImportVorschlag;

public class HbciImportDialogController {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private final AppContext ctx;
    private final ObservableList<ImportZeile> zeilen = FXCollections.observableArrayList();

    private Konto konto;
    private boolean importiert;

    @FXML private Label kontoLabel;
    @FXML private DatePicker vonPicker;
    @FXML private DatePicker bisPicker;
    @FXML private PasswordField pinField;
    @FXML private CheckBox pinMerkenCheck;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label fehlerLabel;
    @FXML private Label statusLabel;
    @FXML private TableView<ImportZeile> ergebnisTable;
    @FXML private TableColumn<ImportZeile, Boolean> uebernehmenColumn;
    @FXML private TableColumn<ImportZeile, String> datumColumn;
    @FXML private TableColumn<ImportZeile, String> betragColumn;
    @FXML private TableColumn<ImportZeile, String> verwendungszweckColumn;
    @FXML private TableColumn<ImportZeile, String> gegenkontoColumn;
    @FXML private TableColumn<ImportZeile, Kategorie> kategorieColumn;
    @FXML private TableColumn<ImportZeile, String> vorhandenColumn;
    @FXML private Button importierenButton;

    public HbciImportDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    public void setKonto(Konto konto) {
        this.konto = konto;
        kontoLabel.setText("Konto: " + konto.getBezeichnung() + " (" + konto.getIban() + ")");
        vonPicker.setValue(konto.getSaldoStandDatum() != null ? konto.getSaldoStandDatum() : LocalDate.now().minusDays(30));
        bisPicker.setValue(LocalDate.now());
        ctx.credentialStore.holePin(konto.getId()).ifPresent(pin -> {
            pinField.setText(pin);
            pinMerkenCheck.setSelected(true);
        });
    }

    public void setKategorien(List<Kategorie> kategorien) {
        kategorieColumn.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(kategorien)));
    }

    public boolean isImportiert() {
        return importiert;
    }

    @FXML
    private void initialize() {
        uebernehmenColumn.setCellValueFactory(cd -> cd.getValue().uebernehmenProperty());
        uebernehmenColumn.setCellFactory(CheckBoxTableCell.forTableColumn(uebernehmenColumn));

        datumColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().buchung().getBuchungsdatum().format(DATUM_FORMAT)));
        betragColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(BETRAG_FORMAT.format(cd.getValue().buchung().getBetragEuro())));
        verwendungszweckColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().buchung().getVerwendungszweck()));
        gegenkontoColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().buchung().getGegenkontoName()));
        vorhandenColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().bereitsVorhanden() ? "Ja" : ""));

        kategorieColumn.setCellValueFactory(cd -> cd.getValue().kategorieProperty());
        kategorieColumn.setOnEditCommit(event -> event.getRowValue().kategorieProperty().setValue(event.getNewValue()));

        ergebnisTable.setItems(zeilen);
    }

    @FXML
    private void onAbrufen() {
        fehlerLabel.setText("");
        LocalDate von = vonPicker.getValue();
        LocalDate bis = bisPicker.getValue();
        String pin = pinField.getText();
        if (von == null || bis == null || von.isAfter(bis)) {
            fehlerLabel.setText("Bitte einen gueltigen Zeitraum waehlen");
            return;
        }
        if (pin == null || pin.isBlank()) {
            fehlerLabel.setText("Bitte PIN eingeben");
            return;
        }

        progressIndicator.setVisible(true);
        importierenButton.setDisable(true);
        zeilen.clear();

        Task<List<HbciImportVorschlag>> task = new Task<>() {
            @Override
            protected List<HbciImportVorschlag> call() {
                return ctx.hbciImportService.holeUmsaetze(konto, pin, von, bis, HbciImportDialogController.this::promptTan);
            }
        };
        task.setOnSucceeded(e -> {
            progressIndicator.setVisible(false);
            List<HbciImportVorschlag> vorschlaege = task.getValue();
            for (HbciImportVorschlag v : vorschlaege) {
                zeilen.add(new ImportZeile(v));
            }
            importierenButton.setDisable(zeilen.isEmpty());
            statusLabel.setText(vorschlaege.size() + " Umsaetze abgerufen");
            speichereOderLoeschePin(pin);
        });
        task.setOnFailed(e -> {
            progressIndicator.setVisible(false);
            Throwable ex = task.getException();
            fehlerLabel.setText("HBCI-Abruf fehlgeschlagen: " + (ex != null ? ex.getMessage() : "unbekannter Fehler"));
        });

        Thread thread = new Thread(task, "hbci-import");
        thread.setDaemon(true);
        thread.start();
    }

    private void speichereOderLoeschePin(String pin) {
        if (pinMerkenCheck.isSelected()) {
            ctx.credentialStore.speicherePin(konto.getId(), pin);
        } else {
            ctx.credentialStore.loeschePin(konto.getId());
        }
    }

    /** Wird vom HBCI-Hintergrundthread aufgerufen (siehe TanCallback) - blockiert diesen, bis der
     * Nutzer die TAN im auf dem JavaFX-Thread angezeigten Dialog eingegeben hat. */
    private String promptTan(String hinweis) {
        CompletableFuture<Optional<String>> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("TAN erforderlich");
            dialog.setHeaderText("Die Bank verlangt eine TAN-Bestaetigung");
            dialog.setContentText(hinweis != null ? hinweis : "Bitte TAN eingeben:");
            future.complete(dialog.showAndWait());
        });
        try {
            return future.get().orElseThrow(() -> new HbciException("TAN-Eingabe abgebrochen"));
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new HbciException("TAN-Eingabe fehlgeschlagen", e);
        }
    }

    @FXML
    private void onImportieren() {
        List<Buchung> ausgewaehlt = zeilen.stream()
                .filter(z -> z.uebernehmenProperty().get())
                .peek(z -> z.buchung().setKategorieId(z.kategorieProperty().get() != null ? z.kategorieProperty().get().getId() : null))
                .map(ImportZeile::buchung)
                .toList();

        int anzahl = ctx.buchungService.importiereHbciBuchungen(ausgewaehlt);
        importiert = anzahl > 0;

        Alert info = new Alert(Alert.AlertType.INFORMATION, anzahl + " Buchungen importiert.");
        info.setHeaderText("Import abgeschlossen");
        info.showAndWait();
        schliessen();
    }

    @FXML
    private void onSchliessen() {
        schliessen();
    }

    private void schliessen() {
        ((Stage) kontoLabel.getScene().getWindow()).close();
    }

    private static class ImportZeile {
        private final HbciImportVorschlag vorschlag;
        private final SimpleBooleanProperty uebernehmen;
        private final SimpleObjectProperty<Kategorie> kategorie = new SimpleObjectProperty<>();

        ImportZeile(HbciImportVorschlag vorschlag) {
            this.vorschlag = vorschlag;
            this.uebernehmen = new SimpleBooleanProperty(!vorschlag.bereitsVorhanden());
        }

        Buchung buchung() {
            return vorschlag.buchung();
        }

        boolean bereitsVorhanden() {
            return vorschlag.bereitsVorhanden();
        }

        SimpleBooleanProperty uebernehmenProperty() {
            return uebernehmen;
        }

        SimpleObjectProperty<Kategorie> kategorieProperty() {
            return kategorie;
        }
    }
}
