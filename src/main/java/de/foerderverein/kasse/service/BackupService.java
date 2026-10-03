package de.foerderverein.kasse.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.foerderverein.kasse.config.AppConfig;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;

/** Datensicherung: eine konsistente Kopie der Datenbankdatei sowie ein CSV-Export aller
 * Buchungen als formatunabhaengige Zweitsicherung, falls die SQLite-Datei einmal nicht
 * mehr lesbar sein sollte. */
public class BackupService {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Database database;
    private final BuchungRepository buchungRepository;
    private final KontoRepository kontoRepository;
    private final KategorieRepository kategorieRepository;
    private final Path belegeVerzeichnis;

    public BackupService(Database database, BuchungRepository buchungRepository, KontoRepository kontoRepository,
                          KategorieRepository kategorieRepository, Path belegeVerzeichnis) {
        this.database = database;
        this.buchungRepository = buchungRepository;
        this.kontoRepository = kontoRepository;
        this.kategorieRepository = kategorieRepository;
        this.belegeVerzeichnis = belegeVerzeichnis;
    }

    /** Erstellt eine konsistente Kopie der Datenbank am Zielpfad. Nutzt SQLite's VACUUM INTO
     * statt eines reinen Datei-Kopiervorgangs, damit die Sicherung auch bei einer (theoretisch)
     * gerade offenen Transaktion konsistent ist, und komprimiert dabei nebenbei die Datei.
     * Kopiert zusaetzlich das Belege-Verzeichnis als Geschwisterordner neben die Zieldatei - seit
     * Belege als Dateien statt als BLOB gespeichert werden, deckt VACUUM INTO allein nur noch
     * die Datenbank selbst ab. */
    public void sichereDatenbank(Path ziel) throws IOException {
        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        Files.deleteIfExists(ziel);
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement("VACUUM INTO ?")) {
            ps.setString(1, ziel.toAbsolutePath().toString());
            ps.execute();
        } catch (SQLException e) {
            throw new IOException("Datenbank-Sicherung konnte nicht erstellt werden", e);
        }
        sichereBelegeVerzeichnis(ziel);
    }

    private void sichereBelegeVerzeichnis(Path zielDatenbankdatei) throws IOException {
        if (!Files.isDirectory(belegeVerzeichnis)) {
            return;
        }
        Path zielVerzeichnis = AppConfig.belegeVerzeichnis(zielDatenbankdatei);
        try (var dateien = Files.list(belegeVerzeichnis)) {
            for (Path datei : dateien.toList()) {
                Files.copy(datei, zielVerzeichnis.resolve(datei.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public void exportiereAlleBuchungen(Path ziel) throws IOException {
        Map<Long, Konto> kontenById = new HashMap<>();
        for (Konto k : kontoRepository.findAll()) {
            kontenById.put(k.getId(), k);
        }
        Map<Long, Kategorie> kategorienById = new HashMap<>();
        for (Kategorie k : kategorieRepository.findAll()) {
            kategorienById.put(k.getId(), k);
        }

        List<Buchung> buchungen = buchungRepository.findAll();
        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(ziel.toFile()), StandardCharsets.UTF_8)) {
            writer.write("Datum;Konto;Kontoauszug;Betrag;Kategorie;Verwendungszweck;Rechnungsnummer;"
                    + "Gegenkonto IBAN;Gegenkonto Name;Quelle;Storniert;Erstellt von;Erstellt am\n");
            for (Buchung b : buchungen) {
                Konto konto = kontenById.get(b.getKontoId());
                Kategorie kategorie = kategorienById.get(b.getKategorieId());
                writer.write(csv(b.getBuchungsdatum().format(DATUM_FORMAT)) + ";"
                        + csv(konto != null ? konto.getBezeichnung() : "") + ";"
                        + csv(b.getKontoauszug()) + ";"
                        + csv(b.getBetragEuro().toPlainString()) + ";"
                        + csv(kategorie != null ? kategorie.getName() : "") + ";"
                        + csv(b.getVerwendungszweck()) + ";"
                        + csv(b.getRechnungsnummer()) + ";"
                        + csv(b.getGegenkontoIban()) + ";"
                        + csv(b.getGegenkontoName()) + ";"
                        + csv(b.getQuelle().name()) + ";"
                        + csv(b.isStorniert() ? "Ja" : "Nein") + ";"
                        + csv(b.getErstelltVon()) + ";"
                        + csv(b.getErstelltAm() != null ? b.getErstelltAm().toString() : "") + "\n");
            }
        }
    }

    /** Wie exportiereAlleBuchungen(...), aber als Excel-Datei fuer die direkte Weiterverarbeitung
     * (Betrag als echte Zahl, damit sich die Spalte in Excel direkt summieren laesst) - anders
     * als der CSV-Export hier bewusst keine formatunabhaengige Zweitsicherung, siehe
     * Klassenkommentar. */
    public void exportiereAlleBuchungenAlsExcel(Path ziel) throws IOException {
        Map<Long, Konto> kontenById = new HashMap<>();
        for (Konto k : kontoRepository.findAll()) {
            kontenById.put(k.getId(), k);
        }
        Map<Long, Kategorie> kategorienById = new HashMap<>();
        for (Kategorie k : kategorieRepository.findAll()) {
            kategorienById.put(k.getId(), k);
        }

        List<Buchung> buchungen = buchungRepository.findAll();
        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);

        String[] spalten = {"Datum", "Konto", "Kontoauszug", "Betrag", "Kategorie", "Verwendungszweck",
                "Rechnungsnummer", "Gegenkonto IBAN", "Gegenkonto Name", "Quelle", "Storniert", "Erstellt von",
                "Erstellt am"};

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Kontobewegungen");

            org.apache.poi.ss.usermodel.Font kopfSchrift = workbook.createFont();
            kopfSchrift.setBold(true);
            CellStyle kopfStil = workbook.createCellStyle();
            kopfStil.setFont(kopfSchrift);

            Row kopfzeile = sheet.createRow(0);
            for (int i = 0; i < spalten.length; i++) {
                Cell zelle = kopfzeile.createCell(i);
                zelle.setCellValue(spalten[i]);
                zelle.setCellStyle(kopfStil);
            }

            int zeilenIndex = 1;
            for (Buchung b : buchungen) {
                Konto konto = kontenById.get(b.getKontoId());
                Kategorie kategorie = kategorienById.get(b.getKategorieId());
                Row zeile = sheet.createRow(zeilenIndex++);
                zeile.createCell(0).setCellValue(b.getBuchungsdatum().format(DATUM_FORMAT));
                zeile.createCell(1).setCellValue(konto != null ? konto.getBezeichnung() : "");
                zeile.createCell(2).setCellValue(wert(b.getKontoauszug()));
                zeile.createCell(3).setCellValue(b.getBetragEuro().doubleValue());
                zeile.createCell(4).setCellValue(kategorie != null ? kategorie.getName() : "");
                zeile.createCell(5).setCellValue(wert(b.getVerwendungszweck()));
                zeile.createCell(6).setCellValue(wert(b.getRechnungsnummer()));
                zeile.createCell(7).setCellValue(wert(b.getGegenkontoIban()));
                zeile.createCell(8).setCellValue(wert(b.getGegenkontoName()));
                zeile.createCell(9).setCellValue(b.getQuelle().name());
                zeile.createCell(10).setCellValue(b.isStorniert() ? "Ja" : "Nein");
                zeile.createCell(11).setCellValue(wert(b.getErstelltVon()));
                zeile.createCell(12).setCellValue(b.getErstelltAm() != null ? b.getErstelltAm().toString() : "");
            }

            for (int i = 0; i < spalten.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(ziel.toFile())) {
                workbook.write(out);
            }
        }
    }

    private String wert(String text) {
        return text != null ? text : "";
    }

    private String csv(String wert) {
        if (wert == null) {
            return "";
        }
        String bereinigt = wert.replace("\"", "\"\"");
        return bereinigt.contains(";") || bereinigt.contains("\"") ? "\"" + bereinigt + "\"" : bereinigt;
    }
}
