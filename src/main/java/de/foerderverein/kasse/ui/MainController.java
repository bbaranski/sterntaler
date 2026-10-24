package de.foerderverein.kasse.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
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

    private static final String HANDBUCH_RESSOURCE = "/de/foerderverein/kasse/doc/handbuch.pdf";

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
    private void onSerienbriefvorlagen() {
        try {
            FXMLLoader loader = Views.loader("/de/foerderverein/kasse/ui/BriefvorlageDialog.fxml");
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Serienbriefvorlagen verwalten");
            stage.setScene(Views.scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            zeigeFehler("Serienbriefvorlagen konnten nicht geoeffnet werden", e);
        }
    }

    @FXML
    private void onKassenbericht() {
        waehleKontoUndJahr("Kassenbericht", (konto, jahr) -> {
            String dateiname = jahr.getBezeichnung().replaceAll("[\\\\/:*?\"<>|]", "_");
            File datei = waehleZieldatei("Kassenbericht speichern", "Kassenbericht-" + dateiname + ".pdf");
            if (datei == null) {
                return;
            }
            try {
                ctx.berichtService.erstelleKassenbericht(datei.toPath(), konto, jahr);
                oeffneDatei(datei);
            } catch (IOException e) {
                zeigeFehler("Kassenbericht konnte nicht erstellt werden", e);
            }
        });
    }

    /** Alle Buchungen eines Geschaeftsjahres einzeln - erscheint in der Oberflaeche als
     * "Kassenprüfbericht", im Code weiter als Buchungsjournal (siehe
     * BerichtService.erstelleBuchungsjournal). Querformat. */
    @FXML
    private void onBuchungsjournal() {
        waehleKontoUndJahr("Kassenprüfbericht", (konto, jahr) -> {
            String dateiname = jahr.getBezeichnung().replaceAll("[\\\\/:*?\"<>|]", "_");
            File datei = waehleZieldatei("Kassenprüfbericht speichern",
                    "Kassenprüfbericht-" + dateiname + ".pdf");
            if (datei == null) {
                return;
            }
            try {
                ctx.berichtService.erstelleBuchungsjournal(datei.toPath(), konto, jahr);
                oeffneDatei(datei);
            } catch (IOException e) {
                zeigeFehler("Kassenprüfbericht konnte nicht erstellt werden", e);
            }
        });
    }

    private interface BerichtAuftrag {
        void ausfuehren(Konto konto, Geschaeftsjahr jahr);
    }

    /** Fragt Konto und Geschaeftsjahr ab und ruft danach den Bericht auf. Bei nur einem Konto
     * entfaellt die Kontoauswahl; bricht der Nutzer einen Dialog ab, passiert nichts. */
    private void waehleKontoUndJahr(String berichtsname, BerichtAuftrag auftrag) {
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
            kontoAuswahl.setTitle(berichtsname);
            kontoAuswahl.setHeaderText("Für welches Konto soll der Bericht erstellt werden?");
            Optional<Konto> gewaehlt = kontoAuswahl.showAndWait();
            if (gewaehlt.isEmpty()) {
                return;
            }
            konto = gewaehlt.get();
        }

        ChoiceDialog<Geschaeftsjahr> jahrAuswahl = new ChoiceDialog<>(jahre.get(0), jahre);
        jahrAuswahl.setTitle(berichtsname);
        jahrAuswahl.setHeaderText("Für welches Geschäftsjahr soll der Bericht erstellt werden?");
        Optional<Geschaeftsjahr> gewaehltesJahr = jahrAuswahl.showAndWait();
        if (gewaehltesJahr.isEmpty()) {
            return;
        }

        auftrag.ausfuehren(konto, gewaehltesJahr.get());
    }

    private File waehleZieldatei(String titel, String vorschlag) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(titel);
        fileChooser.setInitialFileName(vorschlag);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Datei", "*.pdf"));
        return fileChooser.showSaveDialog(tabPane.getScene().getWindow());
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
            kontoAuswahl.setHeaderText("Fuer welches Konto soll die Einnahmen-Überschuss-Rechnung erstellt werden?");
            Optional<Konto> gewaehlt = kontoAuswahl.showAndWait();
            if (gewaehlt.isEmpty()) {
                return;
            }
            konto = gewaehlt.get();
        }

        TextInputDialog jahrDialog = new TextInputDialog(String.valueOf(java.time.LocalDate.now().getYear()));
        jahrDialog.setTitle("Einnahmen-Überschuss-Rechnung");
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
        fileChooser.setTitle("Einnahmen-Überschuss-Rechnung speichern");
        fileChooser.setInitialFileName("Einnahmen-Überschuss-Rechnung-" + jahr + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Datei", "*.pdf"));
        File datei = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            ctx.berichtService.erstelleSteuerreport(datei.toPath(), konto, jahr);
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Einnahmen-Überschuss-Rechnung konnte nicht erstellt werden", e);
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
            ctx.kontobewegungenExportService.exportiereAlleBuchungenAlsExcel(ziel);
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Kontobewegungen konnten nicht exportiert werden", e);
        }
    }

    @FXML
    private void onSicherheitskopie() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Ordner für die Sicherheitskopie wählen");
        File ordner = directoryChooser.showDialog(tabPane.getScene().getWindow());
        if (ordner == null) {
            return;
        }

        try {
            Path erstellt = ctx.sicherheitskopieService.erstelleSicherheitskopie(ordner.toPath());
            oeffneDatei(erstellt.toFile());
        } catch (IOException e) {
            zeigeFehler("Sicherheitskopie konnte nicht erstellt werden", e);
        }
    }

    /** Kennzahlen zu den Mitgliedern als PDF. Anders als Kassenbericht und
     * Einnahmen-Ueberschuss-Rechnung ohne
     * Auswahl von Konto und Geschaeftsjahr - der Bericht ist eine Momentaufnahme des gesamten
     * Mitgliederbestands. */
    @FXML
    private void onMitgliederbericht() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Mitgliederentwicklung speichern");
        fileChooser.setInitialFileName("Mitgliederentwicklung-" + LocalDate.now() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Datei", "*.pdf"));
        File datei = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            ctx.berichtService.erstelleMitgliederbericht(datei.toPath());
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Mitgliederentwicklung konnte nicht erstellt werden", e);
        }
    }

    /** Alle aktiven Mitglieder mit allen Spalten als PDF - wie die Mitgliederentwicklung ohne
     * Auswahl von Konto und Geschaeftsjahr, die Mitglieder haengen an keinem der beiden. */
    @FXML
    private void onMitgliederuebersicht() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Mitgliederübersicht speichern");
        fileChooser.setInitialFileName("Mitgliederübersicht-" + LocalDate.now() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF-Datei", "*.pdf"));
        File datei = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
        if (datei == null) {
            return;
        }

        try {
            ctx.berichtService.erstelleMitgliederuebersicht(datei.toPath());
            oeffneDatei(datei);
        } catch (IOException e) {
            zeigeFehler("Mitgliederübersicht konnte nicht erstellt werden", e);
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
            if (!ergebnis.fehler().isEmpty()) {
                // Es wurde nichts geschrieben, die Tabellen muessen also nicht neu geladen werden.
                zeigeImportFehler(ergebnis.fehler());
                return;
            }
            mitgliederViewController.lade();
            buchungenViewController.ladeStammdaten();

            Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                    ergebnis.importiert() + " Mitglieder importiert.");
            hinweis.setHeaderText("Mitgliederliste importiert");
            hinweis.showAndWait();
        } catch (IOException e) {
            zeigeFehler("Mitgliederliste konnte nicht importiert werden", e);
        }
    }

    /** Die Fehlerliste kann so lang sein wie die Importdatei - daher nicht in den Meldungsrumpf,
     * der den Dialog beliebig hoch werden liesse, sondern in ein scroll- und markierbares
     * Textfeld im aufklappbaren Bereich. */
    private void zeigeImportFehler(List<String> fehler) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                fehler.size() + " Zeile(n) enthalten Fehler. Es wurde nichts importiert.\n"
                        + "Bitte die Datei korrigieren und erneut importieren.");
        alert.setHeaderText("Import nicht möglich");

        TextArea details = new TextArea(String.join("\n", fehler));
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(12);
        details.setPrefColumnCount(60);
        details.setMaxWidth(Double.MAX_VALUE);
        details.setMaxHeight(Double.MAX_VALUE);
        alert.getDialogPane().setExpandableContent(details);
        alert.getDialogPane().setExpanded(true);
        // Lange Meldungen sollen sich durch Aufziehen des Fensters lesbar machen lassen; das
        // Textfeld waechst dank maxWidth/maxHeight mit.
        alert.setResizable(true);
        alert.showAndWait();
    }

    /** Oeffnet die erzeugte Datei bequemlichkeitshalber im Standardprogramm - schlaegt das fehl
     * (z. B. kein Desktop-Support), ist das kein Fehler, die Datei liegt trotzdem am Zielort. */
    /** Zeigt das mitgelieferte Nutzerhandbuch im PDF-Betrachter des Systems. Das PDF liegt als
     * Ressource im Jar (erzeugt aus docs/handbuch.md, siehe HandbuchPdf) und wird dafuer in eine
     * temporaere Datei geschrieben - aus dem Jar heraus laesst sich nichts oeffnen. Bewusst die
     * mitgelieferte Fassung statt eines Links auf GitHub: die Anwendung soll ohne Internet
     * auskommen, und die mitgelieferte Fassung passt immer zur installierten Version. */
    @FXML
    private void onHandbuch() {
        try (InputStream handbuch = getClass().getResourceAsStream(HANDBUCH_RESSOURCE)) {
            if (handbuch == null) {
                Alert hinweis = new Alert(Alert.AlertType.INFORMATION,
                        "Dieser Programmstand enthält kein Handbuch.");
                hinweis.setHeaderText("Handbuch nicht verfügbar");
                hinweis.showAndWait();
                return;
            }
            Path ziel = Files.createTempFile("Nutzerhandbuch-Foerderverein-Kasse", ".pdf");
            Files.copy(handbuch, ziel, StandardCopyOption.REPLACE_EXISTING);
            ziel.toFile().deleteOnExit();
            oeffneDatei(ziel.toFile());
        } catch (IOException e) {
            zeigeFehler("Handbuch konnte nicht geöffnet werden", e);
        }
    }

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
