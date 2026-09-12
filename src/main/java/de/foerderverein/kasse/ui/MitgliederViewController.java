package de.foerderverein.kasse.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Window;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.IbanPruefung;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;

/** Ansicht "Mitglieder" - zweite Hauptansicht der Anwendung, als Tab in MainView.fxml
 * eingebettet (kein eigenes Fenster mehr, siehe ehemals MitgliedDialogController). */
public class MitgliederViewController {

    private final AppContext ctx;
    private final ObservableList<Mitglied> mitglieder = FXCollections.observableArrayList();

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    @FXML private TableView<Mitglied> mitgliederTable;
    @FXML private TableColumn<Mitglied, String> nameColumn;
    @FXML private TableColumn<Mitglied, String> emailColumn;
    @FXML private TableColumn<Mitglied, LocalDate> eintrittsdatumColumn;
    @FXML private TableColumn<Mitglied, LocalDate> austrittsdatumColumn;
    @FXML private TableColumn<Mitglied, String> statusColumn;
    @FXML private TableColumn<Mitglied, String> vornameColumn;
    @FXML private TableColumn<Mitglied, String> nachnameColumn;
    @FXML private TableColumn<Mitglied, String> strasseColumn;
    @FXML private TableColumn<Mitglied, String> plzColumn;
    @FXML private TableColumn<Mitglied, String> ortColumn;
    @FXML private TableColumn<Mitglied, String> notizColumn;
    @FXML private TableColumn<Mitglied, String> ibanColumn;
    @FXML private TableColumn<Mitglied, String> bicColumn;
    @FXML private TableColumn<Mitglied, Long> jahresbeitragColumn;
    @FXML private TableColumn<Mitglied, Long> idColumn;
    @FXML private TextField vornameField;
    @FXML private TextField nachnameField;
    @FXML private TextField strasseField;
    @FXML private TextField plzField;
    @FXML private TextField ortField;
    @FXML private TextField emailField;
    @FXML private DatePicker eintrittsdatumPicker;
    @FXML private DatePicker austrittsdatumPicker;
    @FXML private ComboBox<MitgliedStatus> statusCombo;
    @FXML private TextField ibanField;
    @FXML private Label ibanHinweisLabel;
    @FXML private TextField bicField;
    @FXML private TextField jahresbeitragField;
    @FXML private TextArea notizField;
    @FXML private Label fehlerLabel;
    @FXML private TextField suchFeld;
    @FXML private Button suchZurueckButton;
    @FXML private Button suchWeiterButton;
    @FXML private Label suchTrefferLabel;

    private Mitglied ausgewaehlt;

    public MitgliederViewController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getVollerName()));
        emailColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getEmail()));
        eintrittsdatumColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getEintrittsdatum()));
        eintrittsdatumColumn.setCellFactory(SpaltenFormat.datumZelle(DATUM_FORMAT));
        austrittsdatumColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getAustrittsdatum()));
        austrittsdatumColumn.setCellFactory(SpaltenFormat.datumZelle(DATUM_FORMAT));
        statusColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getStatus().name()));
        vornameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getVorname()));
        nachnameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getNachname()));
        strasseColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getStrasse()));
        plzColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getPlz()));
        ortColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getOrt()));
        notizColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getNotiz()));
        ibanColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getIban()));
        bicColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBic()));
        jahresbeitragColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getJahresbeitragCent()));
        jahresbeitragColumn.setCellFactory(SpaltenFormat.betragCentZelle(BETRAG_FORMAT));
        idColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getId()));
        statusCombo.setItems(FXCollections.observableArrayList(MitgliedStatus.values()));

        SpaltenKonfiguration.wiederherstellen(mitgliederTable, "mitglieder");

        mitgliederTable.setItems(mitglieder);

        mitgliederTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> zeigeInFormular(sel));

        new TabellenSuche<>(mitgliederTable, suchFeld, suchWeiterButton, suchZurueckButton, suchTrefferLabel,
                this::suchfelderFuer);

        ibanField.textProperty().addListener((obs, alt, neu) -> aktualisiereIbanHinweis());

        lade();
        onNeu();
    }

    /** Fester Satz durchsuchter Felder fuer die Mitglieder-Suche - siehe TabellenSuche. */
    private List<String> suchfelderFuer(Mitglied m) {
        return List.of(
                m.getVollerName() != null ? m.getVollerName() : "",
                m.getEmail() != null ? m.getEmail() : "",
                m.getOrt() != null ? m.getOrt() : "",
                m.getStrasse() != null ? m.getStrasse() : "",
                m.getNotiz() != null ? m.getNotiz() : "",
                m.getIban() != null ? m.getIban() : "",
                m.getBic() != null ? m.getBic() : "");
    }

    /** Rein informativer Hinweis (siehe IbanPruefung) - blockiert das Speichern bewusst nicht,
     * da manche Sonderfaelle (z. B. vorlaeufig unvollstaendige Angaben) trotzdem gespeichert
     * werden koennen sollen. */
    private void aktualisiereIbanHinweis() {
        String eingabe = ibanField.getText();
        boolean ungueltig = eingabe != null && !eingabe.isBlank() && !IbanPruefung.istGueltig(eingabe);
        ibanHinweisLabel.setText(ungueltig ? "IBAN-Prüfsumme ungültig" : "");
        if (ungueltig) {
            if (!ibanField.getStyleClass().contains("warnung-feld")) {
                ibanField.getStyleClass().add("warnung-feld");
            }
        } else {
            ibanField.getStyleClass().remove("warnung-feld");
        }
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
            ibanField.clear();
            bicField.clear();
            jahresbeitragField.clear();
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
        ibanField.setText(mitglied.getIban());
        bicField.setText(mitglied.getBic());
        jahresbeitragField.setText(mitglied.getJahresbeitragEuro() != null
                ? mitglied.getJahresbeitragEuro().toPlainString().replace('.', ',') : "");
        notizField.setText(mitglied.getNotiz());
    }

    @FXML
    private void onSpalten() {
        Window owner = mitgliederTable.getScene().getWindow();
        SpaltenKonfiguration.zeigeDialog(mitgliederTable, "mitglieder", owner);
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
            ziel.setIban(leerZuNull(ibanField.getText()));
            ziel.setBic(leerZuNull(bicField.getText()));
            ziel.setJahresbeitragEuro(parseJahresbeitrag());
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

    private BigDecimal parseJahresbeitrag() {
        String text = jahresbeitragField.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Jahresbeitrag ungueltig, z. B. 24,00");
        }
    }
}
