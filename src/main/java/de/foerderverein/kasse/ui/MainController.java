package de.foerderverein.kasse.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextInputDialog;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.app.Views;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.service.MitgliederImportErgebnis;

/** Shell des Hauptfensters: Menueleiste (Einstellungen/Berichte/Exporte) und die beiden
 * Hauptansichten (Tabs "Buchungen"/"Mitglieder") als eingebettete fx:include-Ansichten.
 * Die eigentliche Fachlogik lebt in BuchungenViewController und MitgliederViewController -
 * hier nur Menue-Handler und die Koordination zwischen ihnen. */
public class MainController {

    private final AppContext ctx;

    @FXML private TabPane tabPane;
    @FXML private BuchungenViewController buchungenViewController;
    @FXML private MitgliederViewController mitgliederViewController;

    public MainController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        // Beim Zurueckwechseln zu "Buchungen" koennen sich Kategorien/Mitglieder/Geschaeftsjahre
        // im Mitglieder-Tab oder ueber die Menue-Dialoge geaendert haben.
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {
            if (neu != null && "Buchungen".equals(neu.getText())) {
                buchungenViewController.ladeStammdaten();
                buchungenViewController.ladeGeschaeftsjahrDaten();
            }
        });
    }

    @FXML
    private void onVereinsdaten() {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/VereinDialog.fxml");
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Vereinsdaten");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Vereinsdaten konnten nicht geoeffnet werden", e);
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
            buchungenViewController.ladeStammdaten();
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
            buchungenViewController.ladeStammdaten();
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
            buchungenViewController.ladeGeschaeftsjahrDaten();
        }
    }

    @FXML
    private void onAktivitaeten() {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/AktivitaetDialog.fxml");
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Aktivitäten/Veranstaltungen verwalten");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Aktivitätenverwaltung konnte nicht geoeffnet werden", e);
        } finally {
            buchungenViewController.ladeStammdaten();
        }
    }

    @FXML
    private void onKassenbericht() {
        List<Konto> konten = ctx.kontoService.findAll();
        if (konten.isEmpty()) {
            Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                    "Es ist noch kein Konto angelegt. Bitte zuerst unter \"Einstellungen → Konto-Einstellungen\" ein Konto anlegen.");
            hinweis.setHeaderText("Konto einrichten");
            hinweis.showAndWait();
            return;
        }
        List<Geschaeftsjahr> jahre = ctx.geschaeftsjahrService.findAll();
        if (jahre.isEmpty()) {
            Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                    "Es ist noch kein Geschäftsjahr angelegt. Bitte zuerst unter \"Einstellungen → Geschäftsjahre…\" eines anlegen.");
            hinweis.setHeaderText("Kein Geschäftsjahr vorhanden");
            hinweis.showAndWait();
            return;
        }

        Konto konto = konten.get(0);
        if (konten.size() > 1) {
            ChoiceDialog<Konto> kontoAuswahl = new ChoiceDialog<>(konto, konten);
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
        File datei = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            Path ziel = datei.toPath();
            ctx.berichtService.erstelleKassenbericht(ziel, konto, gewaehltesJahr.get());
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Kassenbericht konnte nicht erstellt werden", e);
        }
    }

    @FXML
    private void onSteuerreport() {
        List<Konto> konten = ctx.kontoService.findAll();
        if (konten.isEmpty()) {
            Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                    "Es ist noch kein Konto angelegt. Bitte zuerst unter \"Einstellungen → Konto-Einstellungen\" ein Konto anlegen.");
            hinweis.setHeaderText("Konto einrichten");
            hinweis.showAndWait();
            return;
        }

        Konto konto = konten.get(0);
        if (konten.size() > 1) {
            ChoiceDialog<Konto> kontoAuswahl = new ChoiceDialog<>(konto, konten);
            kontoAuswahl.setHeaderText("Fuer welches Konto soll der Steuerreport erstellt werden?");
            Optional<Konto> gewaehlt = kontoAuswahl.showAndWait();
            if (gewaehlt.isEmpty()) {
                return;
            }
            konto = gewaehlt.get();
        }

        TextInputDialog jahrDialog = new TextInputDialog(String.valueOf(java.time.LocalDate.now().getYear()));
        jahrDialog.setTitle("Ergänzung zur Steuererklärung");
        jahrDialog.setHeaderText("Für welches Jahr soll der Bericht erstellt werden?");
        jahrDialog.setContentText("Jahr:");
        Optional<String> eingabe = jahrDialog.showAndWait();
        if (eingabe.isEmpty()) {
            return;
        }

        int jahr;
        try {
            jahr = Integer.parseInt(eingabe.get().trim());
            if (jahr < 2000 || jahr > 2100) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            Alert hinweis = new Alert(Alert.AlertType.ERROR, "Bitte eine gültige vierstellige Jahreszahl eingeben.");
            hinweis.setHeaderText("Ungültiges Jahr");
            hinweis.showAndWait();
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Steuerreport speichern");
        fileChooser.setInitialFileName("Steuerreport-" + jahr + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Datei", "*.pdf"));
        File datei = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            ctx.berichtService.erstelleSteuerreport(datei.toPath(), konto, jahr);
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Steuerreport konnte nicht erstellt werden", e);
        }
    }

    @FXML
    private void onMitgliederlisteExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Mitgliederliste speichern");
        fileChooser.setInitialFileName("Mitgliederliste.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Datei", "*.xlsx"));
        File datei = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            Path ziel = datei.toPath();
            ctx.berichtService.exportiereMitgliederliste(ziel);
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Mitgliederliste konnte nicht exportiert werden", e);
        }
    }

    @FXML
    private void onKontobewegungenExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Kontobewegungen speichern");
        fileChooser.setInitialFileName("Kontobewegungen.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Datei", "*.xlsx"));
        File datei = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            Path ziel = datei.toPath();
            ctx.backupService.exportiereAlleBuchungenAlsExcel(ziel);
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Kontobewegungen konnten nicht exportiert werden", e);
        }
    }

    @FXML
    private void onMitgliederlisteImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Mitgliederliste importieren");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel-Datei", "*.xlsx"));
        File datei = fileChooser.showOpenDialog(tabPane.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            MitgliederImportErgebnis ergebnis = ctx.berichtService.importiereMitgliederliste(datei.toPath());
            mitgliederViewController.lade();
            buchungenViewController.ladeStammdaten();

            StringBuilder text = new StringBuilder(ergebnis.importiert() + " Mitglieder importiert.");
            if (!ergebnis.fehler().isEmpty()) {
                text.append("\n\n").append(ergebnis.fehler().size()).append(" Zeile(n) übersprungen:\n")
                        .append(String.join("\n", ergebnis.fehler()));
            }
            Alert hinweis = new Alert(
                    ergebnis.fehler().isEmpty() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING, text.toString());
            hinweis.setHeaderText("Mitgliederliste importiert");
            hinweis.showAndWait();
        } catch (IOException e) {
            zeigeFehler("Mitgliederliste konnte nicht importiert werden", e);
        }
    }

    @FXML
    private void onDatensicherung() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Ordner fuer die Datensicherung waehlen");
        File ordner = directoryChooser.showDialog(tabPane.getScene().getWindow());
        if (ordner == null) {
            return;
        }

        String zeitstempel = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm"));
        Path dbZiel = ordner.toPath().resolve("kasse-sicherung-" + zeitstempel + ".db");
        Path csvZiel = ordner.toPath().resolve("buchungen-" + zeitstempel + ".csv");

        try {
            ctx.backupService.sichereDatenbank(dbZiel);
            ctx.backupService.exportiereAlleBuchungen(csvZiel);
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

    private void zeigeFehler(String kontext, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR, kontext + ":\n" + e.getMessage());
        alert.setHeaderText("Fehler");
        alert.showAndWait();
    }
}
