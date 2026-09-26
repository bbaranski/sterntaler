package de.foerderverein.kasse.ui;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.app.Views;
import de.foerderverein.kasse.domain.Anrede;
import de.foerderverein.kasse.domain.Mitglied;

/** Ansicht "Mitglieder" - zweite Hauptansicht der Anwendung, als Tab in MainView.fxml
 * eingebettet. Anlegen/Bearbeiten/Loeschen erfolgt ueber Toolbar-Buttons; Anlegen/Bearbeiten
 * oeffnen dafuer MitgliedDialogController als Pop-up (kein eingebetteter Formularbereich mehr -
 * die Tabelle unterstuetzt Mehrfachauswahl fuer das Massenloeschen). */
public class MitgliederViewController {

    private final AppContext ctx;
    private final ObservableList<Mitglied> mitglieder = FXCollections.observableArrayList();

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
    @FXML private TextField suchFeld;
    @FXML private Button suchZurueckButton;
    @FXML private Button suchWeiterButton;
    @FXML private Label suchTrefferLabel;

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
        });

        new TabellenSuche<>(mitgliederTable, suchFeld, suchWeiterButton, suchZurueckButton, suchTrefferLabel,
                this::suchfelderFuer);

        lade();
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
        mitglieder.setAll(ctx.mitgliedService.findAll());
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

        List<String> fehler = new ArrayList<>();
        for (Mitglied m : ausgewaehlt) {
            try {
                ctx.mitgliedService.loeschen(m);
            } catch (IllegalStateException e) {
                fehler.add(m.getVollerName() + ": " + e.getMessage());
            }
        }
        lade();

        if (!fehler.isEmpty()) {
            Alert hinweis = new Alert(Alert.AlertType.WARNING,
                    (ausgewaehlt.size() - fehler.size()) + " von " + ausgewaehlt.size() + " Mitglied(ern) gelöscht.\n\n"
                            + "Nicht gelöscht:\n" + String.join("\n", fehler));
            hinweis.setHeaderText("Einige Mitglieder konnten nicht gelöscht werden");
            hinweis.showAndWait();
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
