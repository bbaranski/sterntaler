package de.foerderverein.kasse.ui;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.app.Views;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;

/** Ansicht "Buchungen" (Kontoverwaltung) - die Hauptansicht der Anwendung. Wird als Tab in
 * MainView.fxml eingebettet; Kategorien-/Konto-/Geschaeftsjahr-Verwaltung sowie Berichte und
 * Exporte werden ueber die Menueleiste der Shell (MainController) angestossen, die nach
 * Aenderungen ladeStammdaten()/ladeGeschaeftsjahrDaten() hier aufruft. */
public class BuchungenViewController {

    private final AppContext ctx;
    private final ObservableList<Buchung> buchungen = FXCollections.observableArrayList();
    private final ObservableList<Kategorie> kategorienFuerAuswahl = FXCollections.observableArrayList();
    private final ObservableList<Mitglied> mitgliederFuerAuswahl = FXCollections.observableArrayList();
    private Map<Long, Kategorie> kategorienById = new HashMap<>();
    private Map<Long, Konto> kontenById = new HashMap<>();
    private Map<Long, Mitglied> mitgliederById = new HashMap<>();
    private List<Geschaeftsjahr> geschaeftsjahre = List.of();
    private Map<Long, Geschaeftsjahr> geschaeftsjahrByBuchungId = Map.of();
    private Map<Long, Long> laufendeSalden = Map.of();

    @FXML private TableView<Buchung> buchungenTable;
    @FXML private TableColumn<Buchung, LocalDate> datumColumn;
    @FXML private TableColumn<Buchung, String> geschaeftsjahrColumn;
    @FXML private TableColumn<Buchung, Long> betragColumn;
    @FXML private TableColumn<Buchung, Long> saldoColumn;
    @FXML private TableColumn<Buchung, Kategorie> kategorieColumn;
    @FXML private TableColumn<Buchung, Mitglied> mitgliedColumn;
    @FXML private TableColumn<Buchung, String> verwendungszweckColumn;
    @FXML private TableColumn<Buchung, String> gegenkontoColumn;
    @FXML private TableColumn<Buchung, String> quelleColumn;
    @FXML private TableColumn<Buchung, String> kontoColumn;
    @FXML private TableColumn<Buchung, LocalDate> wertstellungColumn;
    @FXML private TableColumn<Buchung, String> waehrungColumn;
    @FXML private TableColumn<Buchung, String> gegenkontoIbanColumn;
    @FXML private TableColumn<Buchung, String> storniertColumn;
    @FXML private TableColumn<Buchung, String> erstelltVonColumn;
    @FXML private TableColumn<Buchung, LocalDateTime> erstelltAmColumn;
    @FXML private TableColumn<Buchung, LocalDateTime> geaendertAmColumn;
    @FXML private TableColumn<Buchung, String> hbciHashColumn;
    @FXML private TableColumn<Buchung, Long> idColumn;
    @FXML private Button bearbeitenButton;
    @FXML private Button stornierenButton;
    @FXML private Button verlaufButton;
    @FXML private Label statusLabel;
    @FXML private TextField suchFeld;
    @FXML private Button suchZurueckButton;
    @FXML private Button suchWeiterButton;
    @FXML private Label suchTrefferLabel;

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter ZEITPUNKT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    public BuchungenViewController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        datumColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getBuchungsdatum()));
        datumColumn.setCellFactory(SpaltenFormat.datumZelle(DATUM_FORMAT));
        geschaeftsjahrColumn.setCellValueFactory(cd -> {
            Geschaeftsjahr g = geschaeftsjahrByBuchungId.get(cd.getValue().getId());
            return new ReadOnlyStringWrapper(g != null ? g.getBezeichnung() : "");
        });
        betragColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getBetragCent()));
        betragColumn.setCellFactory(SpaltenFormat.betragCentZelle(BETRAG_FORMAT));
        saldoColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(laufendeSalden.get(cd.getValue().getId())));
        saldoColumn.setCellFactory(SpaltenFormat.betragCentZelle(BETRAG_FORMAT));
        kategorieColumn.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(kategorienById.get(cd.getValue().getKategorieId())));
        kategorieColumn.setCellFactory(ComboBoxTableCell.forTableColumn(kategorienFuerAuswahl));
        kategorieColumn.setOnEditCommit(event -> {
            Buchung b = event.getRowValue();
            Long alt = b.getKategorieId();
            Kategorie neu = event.getNewValue();
            b.setKategorieId(neu != null ? neu.getId() : null);
            try {
                ctx.buchungService.aktualisiere(b);
            } catch (IllegalStateException e) {
                b.setKategorieId(alt);
                zeigeFehler("Kategorie konnte nicht geaendert werden", e);
            }
            buchungenTable.refresh();
        });

        mitgliedColumn.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(mitgliederById.get(cd.getValue().getMitgliedId())));
        mitgliedColumn.setCellFactory(ComboBoxTableCell.forTableColumn(mitgliederFuerAuswahl));
        mitgliedColumn.setOnEditCommit(event -> {
            Buchung b = event.getRowValue();
            Long alt = b.getMitgliedId();
            Mitglied neu = event.getNewValue();
            b.setMitgliedId(neu != null ? neu.getId() : null);
            try {
                ctx.buchungService.aktualisiere(b);
            } catch (IllegalStateException e) {
                b.setMitgliedId(alt);
                zeigeFehler("Mitglied konnte nicht geaendert werden", e);
            }
            buchungenTable.refresh();
        });
        verwendungszweckColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getVerwendungszweck()));
        gegenkontoColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getGegenkontoName()));
        quelleColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getQuelle().name()));
        kontoColumn.setCellValueFactory(cd -> {
            Konto k = kontenById.get(cd.getValue().getKontoId());
            return new ReadOnlyStringWrapper(k != null ? k.getBezeichnung() : "");
        });
        wertstellungColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getWertstellung()));
        wertstellungColumn.setCellFactory(SpaltenFormat.datumZelle(DATUM_FORMAT));
        waehrungColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getWaehrung()));
        gegenkontoIbanColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getGegenkontoIban()));
        storniertColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().isStorniert() ? "Ja" : ""));
        erstelltVonColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getErstelltVon()));
        erstelltAmColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getErstelltAm()));
        erstelltAmColumn.setCellFactory(SpaltenFormat.zeitpunktZelle(ZEITPUNKT_FORMAT));
        geaendertAmColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getGeaendertAm()));
        geaendertAmColumn.setCellFactory(SpaltenFormat.zeitpunktZelle(ZEITPUNKT_FORMAT));
        hbciHashColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getHbciImportHash()));
        idColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getId()));

        SpaltenKonfiguration.wiederherstellen(buchungenTable, "buchungen");

        buchungenTable.setItems(buchungen);
        buchungenTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Buchung item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("storniert"),
                        !empty && item != null && item.isStorniert());
            }
        });
        buchungenTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            bearbeitenButton.setDisable(sel == null);
            stornierenButton.setDisable(sel == null || sel.isStorniert());
            verlaufButton.setDisable(sel == null);
        });

        new TabellenSuche<>(buchungenTable, suchFeld, suchWeiterButton, suchZurueckButton, suchTrefferLabel,
                this::suchfelderFuer);

        ladeStammdaten();
        ladeBuchungen();

        Platform.runLater(this::pruefeKontoVorhanden);
    }

    void pruefeKontoVorhanden() {
        if (kontenById.isEmpty()) {
            Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                    "Es ist noch kein Konto angelegt. Bitte zuerst unter \"Einstellungen → Konto-Einstellungen\" ein Konto anlegen.");
            hinweis.setHeaderText("Konto einrichten");
            hinweis.showAndWait();
        }
    }

    /** Wird von der Shell (MainController) nach Aenderungen an Kategorien/Konto/Mitgliedern
     * (Menue-Dialoge bzw. Mitglieder-Tab) sowie beim Zurueckwechseln in diesen Tab aufgerufen. */
    public void ladeStammdaten() {
        // Alle Kategorien (auch deaktivierte) fuer die Anzeige bereits vergebener Zuordnungen,
        // aber nur aktive zur Auswahl in Buchungsdialog/HBCI-Import (siehe kategorienAktiv()).
        kategorienById = new HashMap<>();
        for (Kategorie k : ctx.kategorieService.findAll()) {
            kategorienById.put(k.getId(), k);
        }
        kontenById = new HashMap<>();
        for (Konto k : ctx.kontoService.findAll()) {
            kontenById.put(k.getId(), k);
        }
        // Analog: alle Mitglieder fuer die Anzeige, nur aktive zur Auswahl (mitgliederAktiv()).
        mitgliederById = new HashMap<>();
        for (Mitglied m : ctx.mitgliedService.findAll()) {
            mitgliederById.put(m.getId(), m);
        }

        // Haelt die Dropdown-Optionen der inline editierbaren Tabellenspalten aktuell.
        kategorienFuerAuswahl.setAll(kategorienAktiv());
        mitgliederFuerAuswahl.setAll(mitgliederAktiv());
        buchungenTable.refresh();
    }

    /** Wird von der Shell nach Aenderungen an Geschaeftsjahren aufgerufen. */
    public void ladeGeschaeftsjahrDaten() {
        geschaeftsjahre = ctx.geschaeftsjahrService.findAll();
        Map<Long, Geschaeftsjahr> zuordnung = new HashMap<>();
        for (Buchung b : buchungen) {
            ctx.geschaeftsjahrService.findeFuer(b.getBuchungsdatum(), geschaeftsjahre)
                    .ifPresent(g -> zuordnung.put(b.getId(), g));
        }
        geschaeftsjahrByBuchungId = zuordnung;
        laufendeSalden = ctx.geschaeftsjahrService.berechneLaufendeSalden(buchungen, geschaeftsjahre);
        buchungenTable.refresh();
    }

    /** Fester Satz durchsuchter Felder fuer die Buchungen-Suche - bewusst unabhaengig von der
     * aktuellen Spaltensichtbarkeit (siehe TabellenSuche), damit das Suchergebnis vorhersehbar
     * bleibt. */
    private List<String> suchfelderFuer(Buchung b) {
        Kategorie kategorie = kategorienById.get(b.getKategorieId());
        Mitglied mitglied = mitgliederById.get(b.getMitgliedId());
        Konto konto = kontenById.get(b.getKontoId());
        return List.of(
                b.getVerwendungszweck() != null ? b.getVerwendungszweck() : "",
                b.getGegenkontoName() != null ? b.getGegenkontoName() : "",
                kategorie != null ? kategorie.getName() : "",
                mitglied != null ? mitglied.getVollerName() : "",
                konto != null ? konto.getBezeichnung() : "",
                BETRAG_FORMAT.format(b.getBetragEuro()),
                b.getBuchungsdatum().format(DATUM_FORMAT));
    }

    private List<Kategorie> kategorienAktiv() {
        return kategorienById.values().stream().filter(Kategorie::isAktiv).toList();
    }

    private List<Mitglied> mitgliederAktiv() {
        return mitgliederById.values().stream().filter(m -> m.getStatus() == MitgliedStatus.AKTIV).toList();
    }

    private void ladeBuchungen() {
        buchungen.setAll(ctx.buchungService.findAll());
        statusLabel.setText(buchungen.size() + " Buchungen");
        ladeGeschaeftsjahrDaten();
    }

    @FXML
    private void onNeueBuchung() {
        if (kontenById.isEmpty()) {
            pruefeKontoVorhanden();
            return;
        }
        BuchungDialogController controller = openBuchungDialog(null);
        if (controller != null && controller.isGespeichert()) {
            ladeBuchungen();
        }
    }

    @FXML
    private void onBearbeiten() {
        Buchung ausgewaehlt = buchungenTable.getSelectionModel().getSelectedItem();
        if (ausgewaehlt == null) {
            return;
        }
        BuchungDialogController controller = openBuchungDialog(ausgewaehlt);
        if (controller != null && controller.isGespeichert()) {
            ladeBuchungen();
        }
    }

    @FXML
    private void onStornieren() {
        Buchung ausgewaehlt = buchungenTable.getSelectionModel().getSelectedItem();
        if (ausgewaehlt == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Buchung vom " + ausgewaehlt.getBuchungsdatum().format(DATUM_FORMAT)
                        + " ueber " + BETRAG_FORMAT.format(ausgewaehlt.getBetragEuro()) + " stornieren?\n"
                        + "Die Buchung bleibt zur Nachvollziehbarkeit sichtbar, wird aber als storniert markiert.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Buchung stornieren");
        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                ctx.buchungService.storniere(ausgewaehlt);
                ladeBuchungen();
            } catch (IllegalStateException e) {
                zeigeFehler("Buchung konnte nicht storniert werden", e);
            }
        }
    }

    @FXML
    private void onVerlauf() {
        Buchung ausgewaehlt = buchungenTable.getSelectionModel().getSelectedItem();
        if (ausgewaehlt == null) {
            return;
        }
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/BuchungVerlaufDialog.fxml");
            Parent root = loader.load();
            BuchungVerlaufDialogController controller = loader.getController();
            controller.setBuchung(ausgewaehlt);

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Verlauf");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Verlauf konnte nicht geoeffnet werden", e);
        }
    }

    @FXML
    private void onHbciAbruf() {
        List<Konto> hbciFaehig = kontenById.values().stream().filter(Konto::isHbciEingerichtet).toList();
        if (hbciFaehig.isEmpty()) {
            Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                    "Fuer kein Konto ist ein HBCI-Zugang eingerichtet. Bitte zuerst unter "
                            + "\"Einstellungen → Konto-Einstellungen\" den HBCI-Zugang hinterlegen.");
            hinweis.setHeaderText("HBCI-Zugang fehlt");
            hinweis.showAndWait();
            return;
        }
        Konto konto = hbciFaehig.get(0);
        if (hbciFaehig.size() > 1) {
            ChoiceDialog<Konto> auswahl = new ChoiceDialog<>(hbciFaehig.get(0), hbciFaehig);
            auswahl.setHeaderText("Fuer welches Konto sollen Umsaetze abgerufen werden?");
            Optional<Konto> gewaehlt = auswahl.showAndWait();
            if (gewaehlt.isEmpty()) {
                return;
            }
            konto = gewaehlt.get();
        }

        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/HbciImportDialog.fxml");
            Parent root = loader.load();
            HbciImportDialogController controller = loader.getController();
            controller.setKonto(konto);
            controller.setKategorien(kategorienAktiv());

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("HBCI-Umsatzabruf");
            stage.setScene(Views.scene(root));
            stage.showAndWait();

            if (controller.isImportiert()) {
                ladeBuchungen();
            }
        } catch (Exception e) {
            zeigeFehler("HBCI-Abruf konnte nicht geoeffnet werden", e);
        }
    }

    @FXML
    private void onSpalten() {
        Window owner = buchungenTable.getScene().getWindow();
        SpaltenKonfiguration.zeigeDialog(buchungenTable, "buchungen", owner);
    }

    private BuchungDialogController openBuchungDialog(Buchung buchung) {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/BuchungDialog.fxml");
            Parent root = loader.load();
            BuchungDialogController controller = loader.getController();
            controller.setDaten(buchung, List.copyOf(kontenById.values()), kategorienAktiv(), mitgliederAktiv());

            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(buchung == null ? "Neue Buchung" : "Buchung bearbeiten");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
            return controller;
        } catch (Exception e) {
            zeigeFehler("Buchungsdialog konnte nicht geoeffnet werden", e);
            return null;
        }
    }

    private void zeigeFehler(String kontext, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, kontext + ":\n" + e.getMessage());
        alert.setHeaderText("Fehler");
        alert.showAndWait();
    }
}
