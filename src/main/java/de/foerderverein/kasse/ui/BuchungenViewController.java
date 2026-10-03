package de.foerderverein.kasse.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.app.Views;
import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Beleg;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;

/** Ansicht "Buchungen" (Kontoverwaltung) - die Hauptansicht der Anwendung. Wird als Tab in
 * MainView.fxml eingebettet; Kategorien-/Konto-/Geschaeftsjahr-Verwaltung sowie Berichte und
 * Exporte werden ueber die Menueleiste der Shell (MainController) angestossen, die nach
 * Aenderungen ladeStammdaten()/ladeGeschaeftsjahrDaten() hier aufruft. */
public class BuchungenViewController {

    private final AppContext ctx;
    private final ObservableList<Buchung> buchungen = FXCollections.observableArrayList();
    private final ObservableList<Kategorie> kategorienFuerAuswahl = FXCollections.observableArrayList();
    private final ObservableList<Aktivitaet> aktivitaetenFuerAuswahl = FXCollections.observableArrayList();
    private Map<Long, Kategorie> kategorienById = new HashMap<>();
    private Map<Long, Konto> kontenById = new HashMap<>();
    private Map<Long, Aktivitaet> aktivitaetenById = new HashMap<>();
    private Map<Long, Beleg> belegeByBuchungId = new HashMap<>();
    private List<Geschaeftsjahr> geschaeftsjahre = List.of();
    private Map<Long, Geschaeftsjahr> geschaeftsjahrByBuchungId = Map.of();
    private Map<Long, Long> laufendeSalden = Map.of();

    @FXML private TableView<Buchung> buchungenTable;
    @FXML private TableColumn<Buchung, LocalDate> datumColumn;
    @FXML private TableColumn<Buchung, String> geschaeftsjahrColumn;
    @FXML private TableColumn<Buchung, String> kontoauszugColumn;
    @FXML private TableColumn<Buchung, Long> betragColumn;
    @FXML private TableColumn<Buchung, Long> saldoColumn;
    @FXML private TableColumn<Buchung, Kategorie> kategorieColumn;
    @FXML private TableColumn<Buchung, Aktivitaet> aktivitaetColumn;
    @FXML private TableColumn<Buchung, String> verwendungszweckColumn;
    @FXML private TableColumn<Buchung, String> gegenkontoColumn;
    @FXML private TableColumn<Buchung, String> quelleColumn;
    @FXML private TableColumn<Buchung, String> rechnungsnummerColumn;
    @FXML private TableColumn<Buchung, Buchung> belegColumn;
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
        kontoauszugColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getKontoauszug()));
        kontoauszugColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        kontoauszugColumn.setOnEditCommit(event -> {
            Buchung b = event.getRowValue();
            String alt = b.getKontoauszug();
            b.setKontoauszug(event.getNewValue());
            try {
                ctx.buchungService.aktualisiere(b);
            } catch (IllegalStateException e) {
                b.setKontoauszug(alt);
                zeigeFehler("Kontoauszug konnte nicht geaendert werden", e);
            }
            buchungenTable.refresh();
        });
        betragColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getBetragCent()));
        betragColumn.setCellFactory(SpaltenFormat.betragCentZelle(BETRAG_FORMAT));
        saldoColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(laufendeSalden.get(cd.getValue().getId())));
        saldoColumn.setCellFactory(SpaltenFormat.betragCentZelle(BETRAG_FORMAT));
        kategorieColumn.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(kategorienById.get(cd.getValue().getKategorieId())));
        kategorieColumn.setCellFactory(col -> kategorieZelle());
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

        aktivitaetColumn.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(aktivitaetenById.get(cd.getValue().getAktivitaetId())));
        aktivitaetColumn.setCellFactory(ComboBoxTableCell.forTableColumn(aktivitaetenFuerAuswahl));
        aktivitaetColumn.setOnEditCommit(event -> {
            Buchung b = event.getRowValue();
            Long alt = b.getAktivitaetId();
            Aktivitaet neu = event.getNewValue();
            b.setAktivitaetId(neu != null ? neu.getId() : null);
            try {
                ctx.buchungService.aktualisiere(b);
            } catch (IllegalStateException e) {
                b.setAktivitaetId(alt);
                zeigeFehler("Aktivität konnte nicht geändert werden", e);
            }
            buchungenTable.refresh();
        });
        verwendungszweckColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getVerwendungszweck()));
        gegenkontoColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getGegenkontoName()));
        quelleColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getQuelle().name()));

        rechnungsnummerColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getRechnungsnummer()));
        rechnungsnummerColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        rechnungsnummerColumn.setOnEditCommit(event -> {
            Buchung b = event.getRowValue();
            String alt = b.getRechnungsnummer();
            b.setRechnungsnummer(event.getNewValue());
            try {
                ctx.buchungService.aktualisiere(b);
            } catch (IllegalStateException e) {
                b.setRechnungsnummer(alt);
                zeigeFehler("Rechnungsnummer konnte nicht geaendert werden", e);
            }
            buchungenTable.refresh();
        });

        belegColumn.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        belegColumn.setCellFactory(col -> new TableCell<Buchung, Buchung>() {
            private final Button hochladenButton = new Button("Hochladen…");
            private final Button oeffnenButton = new Button("Öffnen");
            private final Button ersetzenButton = new Button("Ersetzen…");
            private final HBox ohneBelegBox = new HBox(hochladenButton);
            private final HBox mitBelegBox = new HBox(4, oeffnenButton, ersetzenButton);
            {
                hochladenButton.setOnAction(e -> hochladeDialog(getItem(), false));
                ersetzenButton.setOnAction(e -> hochladeDialog(getItem(), true));
                oeffnenButton.setOnAction(e -> oeffneVorhandenenBeleg(getItem()));
            }

            @Override
            protected void updateItem(Buchung buchung, boolean empty) {
                super.updateItem(buchung, empty);
                if (empty || buchung == null) {
                    setGraphic(null);
                } else if (belegeByBuchungId.containsKey(buchung.getId())) {
                    setGraphic(mitBelegBox);
                } else {
                    setGraphic(ohneBelegBox);
                }
            }
        });

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
        // Bearbeiten/Stornieren bleiben absichtlich dauerhaft deaktiviert (siehe FXML) - Buchungen
        // sollen nicht mehr manuell erstellt oder geaendert werden koennen, die Funktionalitaet
        // selbst (onBearbeiten/onStornieren/BuchungService) bleibt aber unangetastet bestehen.
        buchungenTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) ->
                verlaufButton.setDisable(sel == null));

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

    /** Wird von der Shell (MainController) nach Aenderungen an Kategorien/Konto
     * (Menue-Dialoge) sowie beim Zurueckwechseln in diesen Tab aufgerufen. */
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
        aktivitaetenById = new HashMap<>();
        for (Aktivitaet a : ctx.aktivitaetService.findAll()) {
            aktivitaetenById.put(a.getId(), a);
        }

        // Haelt die Dropdown-Optionen der inline editierbaren Tabellenspalten aktuell.
        kategorienFuerAuswahl.setAll(KategorieAuswahl.mitBereichKoepfen(kategorienAktiv()));
        aktivitaetenFuerAuswahl.setAll(aktivitaetenAktiv());
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
        Konto konto = kontenById.get(b.getKontoId());
        Aktivitaet aktivitaet = aktivitaetenById.get(b.getAktivitaetId());
        return List.of(
                b.getVerwendungszweck() != null ? b.getVerwendungszweck() : "",
                b.getGegenkontoName() != null ? b.getGegenkontoName() : "",
                kategorie != null ? kategorie.getName() : "",
                konto != null ? konto.getBezeichnung() : "",
                aktivitaet != null ? aktivitaet.getName() : "",
                b.getKontoauszug() != null ? b.getKontoauszug() : "",
                b.getRechnungsnummer() != null ? b.getRechnungsnummer() : "",
                BETRAG_FORMAT.format(b.getBetragEuro()),
                b.getBuchungsdatum().format(DATUM_FORMAT));
    }

    private List<Kategorie> kategorienAktiv() {
        return kategorienById.values().stream().filter(Kategorie::isAktiv)
                .sorted(Comparator.comparing(Kategorie::getBereich).thenComparing(Kategorie::getName))
                .toList();
    }

    /** Eigene TableCell statt ComboBoxTableCell.forTableColumn(...), da diese eingebaute Zelle
     * keine Anpassung der Dropdown-Darstellung erlaubt (kein Zugriff auf die intern verwaltete
     * ComboBox) - hier aber gebraucht, um Bereich-Kopfzeilen anzuzeigen (siehe KategorieAuswahl). */
    private TableCell<Buchung, Kategorie> kategorieZelle() {
        return new TableCell<>() {
            private final ComboBox<Kategorie> comboBox = new ComboBox<>(kategorienFuerAuswahl);
            {
                KategorieAuswahl.konfiguriere(comboBox);
                comboBox.setMaxWidth(Double.MAX_VALUE);
                // setOnAction statt eines valueProperty-Listeners: Action-Events feuern nur bei
                // echter Nutzerauswahl (Klick in der Liste/Enter), nicht bei einem programmatischen
                // setValue(...) - genau das brauchen wir hier, da startEdit() den aktuellen Wert per
                // setValue(...) vorbelegt und das sonst sofort einen ungewollten Commit ausloesen
                // wuerde (setValue selbst zaehlt fuer einen valueProperty-Listener als Aenderung).
                comboBox.setOnAction(event -> {
                    if (isEditing()) {
                        commitEdit(comboBox.getValue());
                    }
                });
            }

            @Override
            public void startEdit() {
                if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                    return;
                }
                super.startEdit();
                comboBox.setValue(getItem());
                setText(null);
                setGraphic(comboBox);
                // Muss verzoegert erfolgen: unmittelbar nach setGraphic(...) ist die ComboBox noch
                // nicht vollstaendig im Scene-Graph verankert, show() haette dann keine Wirkung
                // oder platziert das Popup falsch. Erwartetes Verhalten wie beim zuvor genutzten
                // ComboBoxTableCell, das den Dropdown-Vorschlag beim Editier-Start sofort oeffnet.
                comboBox.requestFocus();
                Platform.runLater(comboBox::show);
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() == null ? "" : getItem().getName());
                setGraphic(null);
            }

            @Override
            protected void updateItem(Kategorie item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    comboBox.setValue(item);
                    setText(null);
                    setGraphic(comboBox);
                } else {
                    setText(item == null ? "" : item.getName());
                    setGraphic(null);
                }
            }
        };
    }

    private List<Aktivitaet> aktivitaetenAktiv() {
        return aktivitaetenById.values().stream().filter(Aktivitaet::isAktiv).toList();
    }

    private void ladeBuchungen() {
        buchungen.setAll(ctx.buchungService.findAll());
        belegeByBuchungId = ctx.belegService.findAll();
        statusLabel.setText(buchungen.size() + " Buchungen");
        ladeGeschaeftsjahrDaten();
    }

    private void hochladeDialog(Buchung buchung, boolean ersetzen) {
        if (buchung == null) {
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(ersetzen ? "Beleg ersetzen" : "Beleg hochladen");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF/JPG-Datei", "*.pdf", "*.jpg", "*.jpeg"));
        File datei = fileChooser.showOpenDialog(buchungenTable.getScene().getWindow());
        if (datei == null) {
            return;
        }
        try {
            Beleg beleg = ctx.belegService.hochladen(buchung.getId(), datei);
            belegeByBuchungId.put(buchung.getId(), beleg);
            buchungenTable.refresh();
        } catch (IllegalArgumentException e) {
            zeigeFehler("Beleg konnte nicht hochgeladen werden", e);
        } catch (IOException e) {
            zeigeFehler("Beleg konnte nicht gelesen werden", e);
        }
    }

    private void oeffneVorhandenenBeleg(Buchung buchung) {
        if (buchung == null) {
            return;
        }
        ctx.belegService.findByBuchungId(buchung.getId()).ifPresentOrElse(
                this::oeffneBeleg,
                () -> zeigeFehler("Beleg konnte nicht geoeffnet werden",
                        new IllegalStateException("Kein Beleg vorhanden")));
    }

    private void oeffneBeleg(Beleg beleg) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(ctx.belegService.pfadZuDatei(beleg).toFile());
            }
        } catch (IOException e) {
            zeigeFehler("Beleg konnte nicht geoeffnet werden", e);
        }
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
            controller.setDaten(buchung, List.copyOf(kontenById.values()), kategorienAktiv(), aktivitaetenAktiv());

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
