package de.foerderverein.kasse.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;

/** Export aller Kontobewegungen (Buchungen) als Excel-Datei fuer die direkte Weiterverarbeitung
 * (Betrag als echte Zahl, damit sich die Spalte in Excel direkt summieren laesst). Enthaelt
 * saemtliche Felder von Buchung (auch die technischen wie HBCI-Import-Hash oder Storno-Referenz) -
 * bewusst keine zusaetzlichen, nur zur Laufzeit berechneten Spalten wie Geschaeftsjahr oder
 * laufendes Saldo, die es in der Tabellenansicht der App gibt, aber kein Feld von Buchung selbst
 * sind. */
public class KontobewegungenExportService {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter ZEITPUNKT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final BuchungRepository buchungRepository;
    private final KontoRepository kontoRepository;
    private final KategorieRepository kategorieRepository;
    private final AktivitaetRepository aktivitaetRepository;

    public KontobewegungenExportService(BuchungRepository buchungRepository, KontoRepository kontoRepository,
                                         KategorieRepository kategorieRepository,
                                         AktivitaetRepository aktivitaetRepository) {
        this.buchungRepository = buchungRepository;
        this.kontoRepository = kontoRepository;
        this.kategorieRepository = kategorieRepository;
        this.aktivitaetRepository = aktivitaetRepository;
    }

    public void exportiereAlleBuchungenAlsExcel(Path ziel) throws IOException {
        Map<Long, Konto> kontenById = new HashMap<>();
        for (Konto k : kontoRepository.findAll()) {
            kontenById.put(k.getId(), k);
        }
        Map<Long, Kategorie> kategorienById = new HashMap<>();
        for (Kategorie k : kategorieRepository.findAll()) {
            kategorienById.put(k.getId(), k);
        }
        Map<Long, Aktivitaet> aktivitaetenById = new HashMap<>();
        for (Aktivitaet a : aktivitaetRepository.findAll()) {
            aktivitaetenById.put(a.getId(), a);
        }

        List<Buchung> buchungen = buchungRepository.findAll();
        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);

        String[] spalten = {"Id", "Datum", "Wertstellung", "Konto", "Kontoauszug", "Betrag", "Währung", "Kategorie",
                "Aktivität", "Verwendungszweck", "Rechnungsnummer", "Gegenkonto IBAN", "Gegenkonto Name", "Quelle",
                "HBCI-Import-Hash", "Storniert", "Storno-Referenz-Id", "Erstellt von", "Erstellt am", "Geändert am"};

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
                Aktivitaet aktivitaet = aktivitaetenById.get(b.getAktivitaetId());
                Row zeile = sheet.createRow(zeilenIndex++);
                zeile.createCell(0).setCellValue(b.getId());
                zeile.createCell(1).setCellValue(b.getBuchungsdatum().format(DATUM_FORMAT));
                zeile.createCell(2).setCellValue(b.getWertstellung() != null ? b.getWertstellung().format(DATUM_FORMAT) : "");
                zeile.createCell(3).setCellValue(konto != null ? konto.getBezeichnung() : "");
                zeile.createCell(4).setCellValue(wert(b.getKontoauszug()));
                zeile.createCell(5).setCellValue(b.getBetragEuro().doubleValue());
                zeile.createCell(6).setCellValue(wert(b.getWaehrung()));
                zeile.createCell(7).setCellValue(kategorie != null ? kategorie.getName() : "");
                zeile.createCell(8).setCellValue(aktivitaet != null ? aktivitaet.getName() : "");
                zeile.createCell(9).setCellValue(wert(b.getVerwendungszweck()));
                zeile.createCell(10).setCellValue(wert(b.getRechnungsnummer()));
                zeile.createCell(11).setCellValue(wert(b.getGegenkontoIban()));
                zeile.createCell(12).setCellValue(wert(b.getGegenkontoName()));
                zeile.createCell(13).setCellValue(b.getQuelle().name());
                zeile.createCell(14).setCellValue(wert(b.getHbciImportHash()));
                zeile.createCell(15).setCellValue(b.isStorniert() ? "Ja" : "Nein");
                if (b.getStornoReferenzId() != null) {
                    zeile.createCell(16).setCellValue(b.getStornoReferenzId());
                }
                zeile.createCell(17).setCellValue(wert(b.getErstelltVon()));
                zeile.createCell(18).setCellValue(b.getErstelltAm() != null ? b.getErstelltAm().format(ZEITPUNKT_FORMAT) : "");
                zeile.createCell(19).setCellValue(b.getGeaendertAm() != null ? b.getGeaendertAm().format(ZEITPUNKT_FORMAT) : "");
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
}
