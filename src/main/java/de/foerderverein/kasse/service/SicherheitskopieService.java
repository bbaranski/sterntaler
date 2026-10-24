package de.foerderverein.kasse.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TreeSet;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.BuchungRepository;

/** Erstellt an einem frei waehlbaren Ort einen mit Datum/Uhrzeit benannten Unterordner und legt
 * darin einen vollstaendigen, menschlich nachvollziehbaren Schnappschuss der Vereinsdaten ab:
 * Buchungen und Mitgliederliste als Excel, saemtliche Berichte als PDF, alle Beleg-Dateien sowie
 * die Original-Datenbankdatei als 1:1-Kopie. Kombiniert dafuer bewusst nur bereits vorhandene
 * Bausteine (KontobewegungenExportService, BerichtService, Belege-/Datenbankdatei), statt
 * Exportlogik zu duplizieren.
 *
 * <p>Die Berichte liegen in einem Unterordner "Berichte" und sind der Teil der Sicherung, der
 * sich auch in zehn Jahren noch ohne diese Anwendung lesen laesst - die .db-Datei braucht das
 * Programm, ein PDF nicht. */
public class SicherheitskopieService {

    private static final DateTimeFormatter ORDNER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

    private final KontobewegungenExportService kontobewegungenExportService;
    private final BerichtService berichtService;
    private final KontoService kontoService;
    private final GeschaeftsjahrService geschaeftsjahrService;
    private final BuchungRepository buchungRepository;
    private final Path belegeVerzeichnis;
    private final Path databaseFile;

    public SicherheitskopieService(KontobewegungenExportService kontobewegungenExportService,
                                    BerichtService berichtService, KontoService kontoService,
                                    GeschaeftsjahrService geschaeftsjahrService,
                                    BuchungRepository buchungRepository,
                                    Path belegeVerzeichnis, Path databaseFile) {
        this.kontobewegungenExportService = kontobewegungenExportService;
        this.berichtService = berichtService;
        this.kontoService = kontoService;
        this.geschaeftsjahrService = geschaeftsjahrService;
        this.buchungRepository = buchungRepository;
        this.belegeVerzeichnis = belegeVerzeichnis;
        this.databaseFile = databaseFile;
    }

    /** Legt unter zielOrdner einen neuen, nach dem aktuellen Zeitpunkt benannten Unterordner an
     * und befuellt ihn. Bricht ein Teilschritt ab, bleiben die bis dahin bereits geschriebenen
     * Dateien unveraendert liegen - kein Rollback, passend zur sonstigen Fehlerbehandlung der
     * Anwendung. */
    public Path erstelleSicherheitskopie(Path zielOrdner) throws IOException {
        Path sicherheitskopieOrdner = zielOrdner.resolve(LocalDateTime.now().format(ORDNER_FORMAT));
        Files.createDirectories(sicherheitskopieOrdner);

        kontobewegungenExportService.exportiereAlleBuchungenAlsExcel(
                sicherheitskopieOrdner.resolve("Kontobewegungen.xlsx"));
        berichtService.exportiereMitgliederliste(sicherheitskopieOrdner.resolve("Mitgliederliste.xlsx"));
        schreibeBerichte(sicherheitskopieOrdner.resolve("Berichte"));
        kopiereBelege(sicherheitskopieOrdner);
        Files.copy(databaseFile, sicherheitskopieOrdner.resolve(databaseFile.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);

        return sicherheitskopieOrdner;
    }

    /** Alle Berichte, die sich aus dem aktuellen Datenbestand erzeugen lassen: je Konto und
     * Geschaeftsjahr ein Kassenbericht und ein Kassenpruefbericht, je Konto und Kalenderjahr mit
     * Buchungen eine Einnahmen-Ueberschuss-Rechnung, dazu die beiden Mitgliederberichte, die
     * weder Konto noch Jahr brauchen.
     *
     * <p>Die Einnahmen-Ueberschuss-Rechnung richtet sich nach Kalenderjahren, nicht nach
     * Geschaeftsjahren (siehe BerichtService.erstelleSteuerreport) - deshalb die zweite,
     * eigenstaendig aus den Buchungsdaten ermittelte Jahresliste. */
    private void schreibeBerichte(Path berichteOrdner) throws IOException {
        Files.createDirectories(berichteOrdner);

        berichtService.erstelleMitgliederbericht(berichteOrdner.resolve("Mitgliederentwicklung.pdf"));
        berichtService.erstelleMitgliederuebersicht(berichteOrdner.resolve("Mitgliederuebersicht.pdf"));

        List<Konto> konten = kontoService.findAll();
        List<Geschaeftsjahr> geschaeftsjahre = geschaeftsjahrService.findAll();
        TreeSet<Integer> jahreMitBuchungen = new TreeSet<>();
        for (Buchung buchung : buchungRepository.findAll()) {
            if (buchung.getBuchungsdatum() != null) {
                jahreMitBuchungen.add(buchung.getBuchungsdatum().getYear());
            }
        }

        // Ein Konto im Dateinamen nur dann, wenn es mehrere gibt - sonst steht in jedem Namen
        // derselbe Zusatz.
        boolean mehrereKonten = konten.size() > 1;
        for (Konto konto : konten) {
            String kontoZusatz = mehrereKonten ? "-" + dateinamensicher(konto.getBezeichnung()) : "";
            for (Geschaeftsjahr jahr : geschaeftsjahre) {
                String name = dateinamensicher(jahr.getBezeichnung()) + kontoZusatz + ".pdf";
                berichtService.erstelleKassenbericht(berichteOrdner.resolve("Kassenbericht-" + name),
                        konto, jahr);
                berichtService.erstelleBuchungsjournal(
                        berichteOrdner.resolve("Kassenpruefbericht-" + name), konto, jahr);
            }
            for (int jahr : jahreMitBuchungen) {
                berichtService.erstelleSteuerreport(berichteOrdner.resolve(
                        "Einnahmen-Ueberschuss-Rechnung-" + jahr + kontoZusatz + ".pdf"), konto, jahr);
            }
        }
    }

    /** Bezeichnungen von Konto und Geschaeftsjahr sind freier Text und koennen Zeichen enthalten,
     * die in Dateinamen nicht zulaessig sind. */
    private String dateinamensicher(String bezeichnung) {
        return bezeichnung == null ? "ohne-Bezeichnung" : bezeichnung.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void kopiereBelege(Path sicherheitskopieOrdner) throws IOException {
        if (!Files.isDirectory(belegeVerzeichnis)) {
            return;
        }
        Path belegeZiel = sicherheitskopieOrdner.resolve("Belege");
        try (var dateien = Files.list(belegeVerzeichnis)) {
            for (Path datei : dateien.toList()) {
                Files.createDirectories(belegeZiel);
                Files.copy(datei, belegeZiel.resolve(datei.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
