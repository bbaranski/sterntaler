package de.foerderverein.kasse.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Erstellt an einem frei waehlbaren Ort einen mit Datum/Uhrzeit benannten Unterordner und legt
 * darin einen vollstaendigen, menschlich nachvollziehbaren Schnappschuss der Vereinsdaten ab:
 * Buchungen und Mitgliederliste als Excel, alle Beleg-Dateien sowie die Original-Datenbankdatei
 * als 1:1-Kopie. Kombiniert dafuer bewusst nur bereits vorhandene Bausteine
 * (KontobewegungenExportService, BerichtService, Belege-/Datenbankdatei), statt Exportlogik zu
 * duplizieren. */
public class SicherheitskopieService {

    private static final DateTimeFormatter ORDNER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss");

    private final KontobewegungenExportService kontobewegungenExportService;
    private final BerichtService berichtService;
    private final Path belegeVerzeichnis;
    private final Path databaseFile;

    public SicherheitskopieService(KontobewegungenExportService kontobewegungenExportService,
                                    BerichtService berichtService, Path belegeVerzeichnis, Path databaseFile) {
        this.kontobewegungenExportService = kontobewegungenExportService;
        this.berichtService = berichtService;
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
        kopiereBelege(sicherheitskopieOrdner);
        Files.copy(databaseFile, sicherheitskopieOrdner.resolve(databaseFile.getFileName()),
                StandardCopyOption.REPLACE_EXISTING);

        return sicherheitskopieOrdner;
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
