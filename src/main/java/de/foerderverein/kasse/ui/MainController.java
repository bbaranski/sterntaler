package de.foerderverein.kasse.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.text.NumberFormat;
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.app.Views;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;

public class MainController {

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
    @FXML private TableColumn<Buchung, String> datumColumn;
    @FXML private TableColumn<Buchung, String> geschaeftsjahrColumn;
    @FXML private TableColumn<Buchung, String> betragColumn;
    @FXML private TableColumn<Buchung, String> saldoColumn;
    @FXML private TableColumn<Buchung, Kategorie> kategorieColumn;
    @FXML private TableColumn<Buchung, Mitglied> mitgliedColumn;
    @FXML private TableColumn<Buchung, String> verwendungszweckColumn;
    @FXML private TableColumn<Buchung, String> gegenkontoColumn;
    @FXML private TableColumn<Buchung, String> quelleColumn;
    @FXML private javafx.scene.control.Button bearbeitenButton;
    @FXML private javafx.scene.control.Button stornierenButton;
    @FXML private javafx.scene.control.Button verlaufButton;
    @FXML private Label statusLabel;

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    public MainController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        datumColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getBuchungsdatum().format(DATUM_FORMAT)));
        geschaeftsjahrColumn.setCellValueFactory(cd -> {
            Geschaeftsjahr g = geschaeftsjahrByBuchungId.get(cd.getValue().getId());
            return new ReadOnlyStringWrapper(g != null ? g.getBezeichnung() : "");
        });
        betragColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(BETRAG_FORMAT.format(cd.getValue().getBetragEuro())));
        saldoColumn.setCellValueFactory(cd -> {
            Long saldo = laufendeSalden.get(cd.getValue().getId());
            return new ReadOnlyStringWrapper(saldo != null ? BETRAG_FORMAT.format(BigDecimal.valueOf(saldo, 2)) : "");
        });
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

        ladeStammdaten();
        ladeBuchungen();

        Platform.runLater(this::pruefeKontoVorhanden);
    }

    private void pruefeKontoVorhanden() {
        if (kontenById.isEmpty()) {
            Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                    "Es ist noch kein Konto angelegt. Bitte zuerst unter \"Konto-Einstellungen\" ein Konto anlegen.");
            hinweis.setHeaderText("Konto einrichten");
            hinweis.showAndWait();
        }
    }

    private void ladeStammdaten() {
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

    /** Ordnet jede Buchung dynamisch (ueber das Datum, keine gespeicherte Zuordnung) ihrem
     * Geschaeftsjahr zu und berechnet das laufende Saldo je Buchung neu. Muss nach jeder
     * Aenderung an Buchungen ODER Geschaeftsjahren aufgerufen werden. */
    private void ladeGeschaeftsjahrDaten() {
        geschaeftsjahre = ctx.geschaeftsjahrService.findAll();
        Map<Long, Geschaeftsjahr> zuordnung = new HashMap<>();
        for (Buchung b : buchungen) {
            ctx.geschaeftsjahrService.findeFuer(b.getBuchungsdatum(), geschaeftsjahre)
                    .ifPresent(g -> zuordnung.put(b.getId(), g));
        }
        geschaeftsjahrByBuchungId = zuordnung;
        laufendeSalden = ctx.geschaeftsjahrService.berechneLaufendeSalden(buchungen, geschaeftsjahre);
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
                            + "\"Konto-Einstellungen\" den HBCI-Zugang hinterlegen.");
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
    private void onKategorien() {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/KategorieDialog.fxml");
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Kategorien verwalten");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Kategorienverwaltung konnte nicht geoeffnet werden", e);
        } finally {
            ladeStammdaten();
            buchungenTable.refresh();
        }
    }

    @FXML
    private void onMitglieder() {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/MitgliedDialog.fxml");
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Mitglieder verwalten");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Mitgliederverwaltung konnte nicht geoeffnet werden", e);
        } finally {
            ladeStammdaten();
            buchungenTable.refresh();
        }
    }

    @FXML
    private void onKontoEinstellungen() {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/KontoDialog.fxml");
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Konto-Einstellungen");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Konto-Einstellungen konnten nicht geoeffnet werden", e);
        } finally {
            ladeStammdaten();
            buchungenTable.refresh();
        }
    }

    @FXML
    private void onGeschaeftsjahre() {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/GeschaeftsjahrDialog.fxml");
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Geschäftsjahre verwalten");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Geschäftsjahr-Verwaltung konnte nicht geoeffnet werden", e);
        } finally {
            ladeGeschaeftsjahrDaten();
            buchungenTable.refresh();
        }
    }

    @FXML
    private void onKassenbericht() {
        if (kontenById.isEmpty()) {
            pruefeKontoVorhanden();
            return;
        }
        List<Geschaeftsjahr> jahre = ctx.geschaeftsjahrService.findAll();
        if (jahre.isEmpty()) {
            Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                    "Es ist noch kein Geschäftsjahr angelegt. Bitte zuerst unter \"Geschäftsjahre…\" eines anlegen.");
            hinweis.setHeaderText("Kein Geschäftsjahr vorhanden");
            hinweis.showAndWait();
            return;
        }

        Konto konto = kontenById.values().iterator().next();
        if (kontenById.size() > 1) {
            ChoiceDialog<Konto> kontoAuswahl = new ChoiceDialog<>(konto, kontenById.values());
            kontoAuswahl.setHeaderText("Fuer welches Konto soll der Kassenbericht erstellt werden?");
            Optional<Konto> gewaehlt = kontoAuswahl.showAndWait();
            if (gewaehlt.isEmpty()) {
                return;
            }
            konto = gewaehlt.get();
        }

        ChoiceDialog<Geschaeftsjahr> jahrAuswahl = new ChoiceDialog<>(jahre.get(0), jahre);
        jahrAuswahl.setHeaderText("Fuer welches Geschäftsjahr soll der Kassenbericht erstellt werden?");
        Optional<Geschaeftsjahr> gewaehltesJahr = jahrAuswahl.showAndWait();
        if (gewaehltesJahr.isEmpty()) {
            return;
        }

        String dateiname = gewaehltesJahr.get().getBezeichnung().replaceAll("[\\\\/:*?\"<>|]", "_");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Kassenbericht speichern");
        fileChooser.setInitialFileName("Kassenbericht-" + dateiname + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Datei", "*.pdf"));
        File datei = fileChooser.showSaveDialog(buchungenTable.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            Path ziel = datei.toPath();
            ctx.berichtService.erstelleKassenbericht(ziel, konto, gewaehltesJahr.get());
            statusLabel.setText("Kassenbericht gespeichert: " + ziel);
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Kassenbericht konnte nicht erstellt werden", e);
        }
    }

    @FXML
    private void onMitgliederlisteExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Mitgliederliste speichern");
        fileChooser.setInitialFileName("Mitgliederliste.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV-Datei", "*.csv"));
        File datei = fileChooser.showSaveDialog(buchungenTable.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            Path ziel = datei.toPath();
            ctx.berichtService.exportiereMitgliederliste(ziel);
            statusLabel.setText("Mitgliederliste gespeichert: " + ziel);
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Mitgliederliste konnte nicht exportiert werden", e);
        }
    }

    @FXML
    private void onDatensicherung() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Ordner fuer die Datensicherung waehlen");
        File ordner = directoryChooser.showDialog(buchungenTable.getScene().getWindow());
        if (ordner == null) {
            return;
        }

        String zeitstempel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        Path dbZiel = ordner.toPath().resolve("kasse-sicherung-" + zeitstempel + ".db");
        Path csvZiel = ordner.toPath().resolve("buchungen-" + zeitstempel + ".csv");

        try {
            ctx.backupService.sichereDatenbank(dbZiel);
            ctx.backupService.exportiereAlleBuchungen(csvZiel);
            statusLabel.setText("Datensicherung erstellt: " + ordner);
            oeffneDatei(ordner);
        } catch (IOException e) {
            zeigeFehler("Datensicherung konnte nicht erstellt werden", e);
        }
    }

    /** Oeffnet die erzeugte Datei bequemlichkeitshalber im Standardprogramm - schlaegt das fehl
     * (z. B. kein Desktop-Support), ist das kein Fehler, die Datei liegt trotzdem am Zielort. */
    private void oeffneDatei(File datei) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(datei);
            }
        } catch (Exception e) {
            // Bewusst ignoriert, siehe Methodenkommentar.
        }
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
