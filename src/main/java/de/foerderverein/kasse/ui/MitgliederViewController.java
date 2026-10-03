package de.foerderverein.kasse.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.app.Views;
import de.foerderverein.kasse.domain.Anrede;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;

/** Ansicht "Mitglieder" - zweite Hauptansicht der Anwendung, als Tab in MainView.fxml
 * eingebettet. Anlegen/Bearbeiten/Loeschen erfolgt ueber Toolbar-Buttons; Anlegen/Bearbeiten
 * oeffnen dafuer MitgliedDialogController als Pop-up (kein eingebetteter Formularbereich mehr -
 * die Tabelle unterstuetzt Mehrfachauswahl fuer das Massenloeschen). */
public class MitgliederViewController {

    private final AppContext ctx;
    private final ObservableList<Mitglied> mitglieder = FXCollections.observableArrayList();
    private List<Mitglied> alleMitglieder = List.of();

    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    @FXML private TableView<Mitglied> mitgliederTable;
    @FXML private TableColumn<Mitglied, Long> idColumn;
    @FXML private TableColumn<Mitglied, String> anredeColumn;
    @FXML private TableColumn<Mitglied, String> vornameColumn;
    @FXML private TableColumn<Mitglied, String> nachnameColumn;
    @FXML private TableColumn<Mitglied, String> strasseColumn;
    @FXML private TableColumn<Mitglied, String> plzColumn;
    @FXML private TableColumn<Mitglied, String> ortColumn;
    @FXML private TableColumn<Mitglied, String> emailColumn;
    @FXML private TableColumn<Mitglied, String> eintrittsdatumColumn;
    @FXML private TableColumn<Mitglied, String> austrittsdatumColumn;
    @FXML private TableColumn<Mitglied, String> statusColumn;
    @FXML private TableColumn<Mitglied, Long> jahresbeitragColumn;
    @FXML private TableColumn<Mitglied, String> ibanColumn;
    @FXML private TableColumn<Mitglied, String> bicColumn;
    @FXML private TableColumn<Mitglied, Long> mandatsreferenzColumn;
    @FXML private TableColumn<Mitglied, String> sammellastschriftColumn;
    @FXML private TableColumn<Mitglied, String> notizColumn;
    @FXML private Button bearbeitenButton;
    @FXML private Button loeschenButton;
    @FXML private Button verlaufButton;
    @FXML private ToggleGroup filterGruppe;
    @FXML private ToggleButton filterAlleButton;
    @FXML private ToggleButton filterAktivButton;
    @FXML private ToggleButton filterInaktivButton;
    @FXML private TextField suchFeld;
    @FXML private Button suchZurueckButton;
    @FXML private Button suchWeiterButton;
    @FXML private Label suchTrefferLabel;
    @FXML private Label statusLabel;

    public MitgliederViewController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getId()));
        anredeColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(anredeText(cd.getValue().getAnrede())));
        vornameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getVorname()));
        nachnameColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getNachname()));
        strasseColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getStrasse()));
        plzColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getPlz()));
        ortColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getOrt()));
        emailColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getEmail()));
        eintrittsdatumColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getEintrittsdatum()));
        austrittsdatumColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getAustrittsdatum()));
        statusColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getStatus().name()));
        jahresbeitragColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getJahresbeitragCent()));
        jahresbeitragColumn.setCellFactory(SpaltenFormat.betragCentZelle(BETRAG_FORMAT));
        ibanColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getIban()));
        bicColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBic()));
        mandatsreferenzColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getMandatsreferenz()));
        sammellastschriftColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getSammellastschrift()));
        notizColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getNotiz()));

        SpaltenKonfiguration.wiederherstellen(mitgliederTable, "mitglieder");

        mitgliederTable.setItems(mitglieder);
        mitgliederTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        mitgliederTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<Mitglied>) change -> {
            int anzahl = mitgliederTable.getSelectionModel().getSelectedItems().size();
            bearbeitenButton.setDisable(anzahl != 1);
            loeschenButton.setDisable(anzahl == 0);
            verlaufButton.setDisable(anzahl != 1);
            aktualisiereStatuszeile();
        });
        mitglieder.addListener((ListChangeListener<Mitglied>) change -> aktualisiereStatuszeile());

        filterGruppe.selectedToggleProperty().addListener((obs, alt, neu) -> aktualisiereGefilterteListe());

        new TabellenSuche<>(mitgliederTable, suchFeld, suchWeiterButton, suchZurueckButton, suchTrefferLabel,
                this::suchfelderFuer);

        lade();
    }

    /** Aktuell gewaehlter Statusfilter, oder null fuer "Alle" - siehe Toggle-Buttons in der
     * Toolbar (Alle/Aktive/Inaktive, siehe MitgliederView.fxml). */
    private MitgliedStatus aktuellerFilter() {
        if (filterAktivButton.isSelected()) {
            return MitgliedStatus.AKTIV;
        }
        if (filterInaktivButton.isSelected()) {
            return MitgliedStatus.INAKTIV;
        }
        return null;
    }

    private void aktualisiereGefilterteListe() {
        MitgliedStatus filter = aktuellerFilter();
        mitglieder.setAll(filter == null
                ? alleMitglieder
                : alleMitglieder.stream().filter(m -> m.getStatus() == filter).toList());
    }

    /** Zeigt Anzahl der aktuell ausgewaehlten Mitglieder sowie Summe/Min/Max/Durchschnitt des
     * Jahresbeitrags ueber die gesamte aktuell gefilterte (nicht nur ausgewaehlte) Liste an -
     * Mitglieder ohne hinterlegten Jahresbeitrag werden dabei nicht mitgezaehlt. */
    private void aktualisiereStatuszeile() {
        int ausgewaehlt = mitgliederTable.getSelectionModel().getSelectedItems().size();
        List<BigDecimal> betraege = mitglieder.stream()
                .map(Mitglied::getJahresbeitragEuro)
                .filter(Objects::nonNull)
                .toList();

        StringBuilder text = new StringBuilder(ausgewaehlt + " ausgewählt · " + mitglieder.size() + " angezeigt");
        if (!betraege.isEmpty()) {
            BigDecimal summe = betraege.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal min = betraege.stream().min(Comparator.naturalOrder()).orElseThrow();
            BigDecimal max = betraege.stream().max(Comparator.naturalOrder()).orElseThrow();
            BigDecimal durchschnitt = summe.divide(BigDecimal.valueOf(betraege.size()), 2, RoundingMode.HALF_UP);
            text.append(" · Jahresbeitrag Summe ").append(BETRAG_FORMAT.format(summe))
                    .append(" · Min ").append(BETRAG_FORMAT.format(min))
                    .append(" · Max ").append(BETRAG_FORMAT.format(max))
                    .append(" · Ø ").append(BETRAG_FORMAT.format(durchschnitt));
        }
        statusLabel.setText(text.toString());
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
                m.getBic() != null ? m.getBic() : "",
                m.getMandatsreferenz() != null ? String.valueOf(m.getMandatsreferenz()) : "",
                m.getSammellastschrift() != null ? m.getSammellastschrift() : "");
    }

    private static String anredeText(Anrede anrede) {
        if (anrede == null) {
            return "";
        }
        return switch (anrede) {
            case HERR -> "Herr";
            case FRAU -> "Frau";
            case FAMILIE -> "Familie";
        };
    }

    /** Wird von der Shell (MainController) nach dem Mitgliederlisten-Import aufgerufen, damit neu
     * angelegte Mitglieder ohne Neustart sichtbar werden. */
    public void lade() {
        alleMitglieder = ctx.mitgliedService.findAll();
        aktualisiereGefilterteListe();
    }

    @FXML
    private void onSpalten() {
        Window owner = mitgliederTable.getScene().getWindow();
        SpaltenKonfiguration.zeigeDialog(mitgliederTable, "mitglieder", owner);
    }

    @FXML
    private void onNeu() {
        MitgliedDialogController controller = oeffneDialog(null);
        if (controller != null && controller.isGespeichert()) {
            lade();
        }
    }

    @FXML
    private void onBearbeiten() {
        Mitglied ausgewaehlt = mitgliederTable.getSelectionModel().getSelectedItem();
        if (ausgewaehlt == null) {
            return;
        }
        MitgliedDialogController controller = oeffneDialog(ausgewaehlt);
        if (controller != null && controller.isGespeichert()) {
            lade();
        }
    }

    @FXML
    private void onLoeschen() {
        List<Mitglied> ausgewaehlt = List.copyOf(mitgliederTable.getSelectionModel().getSelectedItems());
        if (ausgewaehlt.isEmpty()) {
            return;
        }
        String frage = ausgewaehlt.size() == 1
                ? "Mitglied \"" + ausgewaehlt.get(0).getVollerName() + "\" wirklich löschen?"
                : ausgewaehlt.size() + " Mitglieder wirklich löschen?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, frage, ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Mitglied(er) löschen");
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        for (Mitglied m : ausgewaehlt) {
            ctx.mitgliedService.loeschen(m);
        }
        lade();
    }

    @FXML
    private void onVerlauf() {
        Mitglied ausgewaehlt = mitgliederTable.getSelectionModel().getSelectedItem();
        if (ausgewaehlt == null) {
            return;
        }
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/MitgliedVerlaufDialog.fxml");
            Parent root = loader.load();
            MitgliedVerlaufDialogController controller = loader.getController();
            controller.setMitglied(ausgewaehlt);

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Verlauf");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Verlauf konnte nicht geoeffnet werden", e);
        }
    }

    private MitgliedDialogController oeffneDialog(Mitglied mitglied) {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/MitgliedDialog.fxml");
            Parent root = loader.load();
            MitgliedDialogController controller = loader.getController();
            controller.setDaten(mitglied);

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(mitglied == null ? "Neues Mitglied" : "Mitglied bearbeiten");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
            return controller;
        } catch (Exception e) {
            zeigeFehler("Mitgliedsdialog konnte nicht geoeffnet werden", e);
            return null;
        }
    }

    private void zeigeFehler(String kontext, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, kontext + ":\n" + e.getMessage());
        alert.setHeaderText("Fehler");
        alert.showAndWait();
    }
}
