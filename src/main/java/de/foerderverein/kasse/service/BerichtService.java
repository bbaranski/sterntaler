package de.foerderverein.kasse.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;

/** Erzeugt die Basis-Berichte fuer die jaehrliche Kassenpruefung: Kassenbericht als PDF und
 * Mitgliederliste als Excel-Datei. Bewusst schlicht gehalten (Tabellen, keine grafische
 * Auswertung) - fuer den vorgesehenen Zweck (Ausdruck/Ablage, Vorlage beim Kassenpruefer,
 * Weiterverarbeitung in Excel) ausreichend. */
public class BerichtService {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private final BuchungRepository buchungRepository;
    private final KategorieRepository kategorieRepository;
    private final MitgliedRepository mitgliedRepository;

    public BerichtService(BuchungRepository buchungRepository, KategorieRepository kategorieRepository,
                           MitgliedRepository mitgliedRepository) {
        this.buchungRepository = buchungRepository;
        this.kategorieRepository = kategorieRepository;
        this.mitgliedRepository = mitgliedRepository;
    }

    public void erstelleKassenbericht(Path ziel, Konto konto, Geschaeftsjahr jahr) throws IOException {
        Map<Long, Kategorie> kategorienById = new LinkedHashMap<>();
        for (Kategorie k : kategorieRepository.findAll()) {
            kategorienById.put(k.getId(), k);
        }

        List<Buchung> buchungen = buchungRepository.findAll().stream()
                .filter(b -> b.getKontoId().equals(konto.getId()))
                .filter(b -> !b.isStorniert())
                .filter(b -> jahr.enthaelt(b.getBuchungsdatum()))
                .sorted(Comparator.comparing(Buchung::getBuchungsdatum))
                .toList();

        Map<String, Long> summeProKategorieEinnahme = new LinkedHashMap<>();
        Map<String, Long> summeProKategorieAusgabe = new LinkedHashMap<>();
        long summeEinnahmen = 0;
        long summeAusgaben = 0;
        for (Buchung b : buchungen) {
            Kategorie k = kategorienById.get(b.getKategorieId());
            String name = k != null ? k.getName() : "(ohne Kategorie)";
            if (b.getBetragCent() >= 0) {
                summeProKategorieEinnahme.merge(name, b.getBetragCent(), Long::sum);
                summeEinnahmen += b.getBetragCent();
            } else {
                summeProKategorieAusgabe.merge(name, -b.getBetragCent(), Long::sum);
                summeAusgaben += -b.getBetragCent();
            }
        }

        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        try (FileOutputStream out = new FileOutputStream(ziel.toFile())) {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titel = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font untertitel = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font abschnitt = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("Kassenbericht " + jahr.getBezeichnung(), titel));
            document.add(new Paragraph(konto.getBezeichnung() + " (" + konto.getIban() + ")", untertitel));
            document.add(new Paragraph("Zeitraum: " + jahr.getStartDatum().format(DATUM_FORMAT)
                    + " - " + jahr.getEndDatum().format(DATUM_FORMAT), untertitel));
            document.add(new Paragraph("Startsaldo: " + BETRAG_FORMAT.format(jahr.getStartsaldoEuro()), untertitel));
            document.add(leerzeile());

            document.add(new Paragraph("Einnahmen", abschnitt));
            document.add(kategorieTabelle(summeProKategorieEinnahme, summeEinnahmen));
            document.add(leerzeile());

            document.add(new Paragraph("Ausgaben", abschnitt));
            document.add(kategorieTabelle(summeProKategorieAusgabe, summeAusgaben));
            document.add(leerzeile());

            long saldoZeitraum = summeEinnahmen - summeAusgaben;
            long endsaldo = jahr.getStartsaldoCent() + saldoZeitraum;
            document.add(new Paragraph("Saldo des Zeitraums: " + BETRAG_FORMAT.format(BigDecimal.valueOf(saldoZeitraum, 2)), abschnitt));
            document.add(new Paragraph("Endsaldo: " + BETRAG_FORMAT.format(BigDecimal.valueOf(endsaldo, 2)), abschnitt));
            document.add(leerzeile());
            document.add(leerzeile());

            document.add(new Paragraph("Erstellt am " + java.time.LocalDate.now().format(DATUM_FORMAT), untertitel));
            document.add(leerzeile());
            document.add(new Paragraph("_______________________________          _______________________________", untertitel));
            document.add(new Paragraph("Datum, Unterschrift Kassenwart/-in          Datum, Unterschrift Kassenpruefer/-in", untertitel));

            // Muss vor dem Schliessen des OutputStreams durch try-with-resources passieren:
            // document.close() schreibt noch Trailer/Xref-Tabelle in den Stream.
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Kassenbericht konnte nicht erzeugt werden", e);
        }
    }

    private Paragraph leerzeile() {
        return new Paragraph(" ");
    }

    private PdfPTable kategorieTabelle(Map<String, Long> summenProKategorie, long gesamtCent) {
        PdfPTable tabelle = new PdfPTable(2);
        tabelle.setWidthPercentage(100);
        try {
            tabelle.setWidths(new float[] {70, 30});
        } catch (DocumentException e) {
            // Feste Spaltenzahl, kann hier nicht auftreten.
        }
        tabelle.addCell(kopfzelle("Kategorie"));
        tabelle.addCell(kopfzelle("Betrag"));
        if (summenProKategorie.isEmpty()) {
            PdfPCell leer = new PdfPCell(new org.openpdf.text.Phrase("(keine Buchungen)"));
            leer.setColspan(2);
            tabelle.addCell(leer);
        }
        for (Map.Entry<String, Long> entry : summenProKategorie.entrySet()) {
            tabelle.addCell(entry.getKey());
            tabelle.addCell(betragZelle(entry.getValue()));
        }
        PdfPCell summeLabel = new PdfPCell(new org.openpdf.text.Phrase("Summe", new Font(Font.HELVETICA, 10, Font.BOLD)));
        tabelle.addCell(summeLabel);
        tabelle.addCell(betragZelle(gesamtCent));
        return tabelle;
    }

    private PdfPCell kopfzelle(String text) {
        PdfPCell cell = new PdfPCell(new org.openpdf.text.Phrase(text, new Font(Font.HELVETICA, 10, Font.BOLD)));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell betragZelle(long cent) {
        PdfPCell cell = new PdfPCell(new org.openpdf.text.Phrase(BETRAG_FORMAT.format(BigDecimal.valueOf(cent, 2))));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private static final String[] MITGLIEDER_SPALTEN = {
            "Vorname", "Nachname", "Straße", "PLZ", "Ort", "E-Mail", "Eintrittsdatum", "Austrittsdatum", "Status"
    };

    /** Excel-Datei (.xlsx) mit allen Mitgliedern - fuer Weitergabe/Weiterverarbeitung ausserhalb
     * der Anwendung (z. B. Serienbriefe, Abgleich mit anderen Listen). */
    public void exportiereMitgliederliste(Path ziel) throws IOException {
        List<Mitglied> mitglieder = mitgliedRepository.findAll();
        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Mitglieder");

            org.apache.poi.ss.usermodel.Font kopfSchrift = workbook.createFont();
            kopfSchrift.setBold(true);
            CellStyle kopfStil = workbook.createCellStyle();
            kopfStil.setFont(kopfSchrift);

            Row kopfzeile = sheet.createRow(0);
            for (int i = 0; i < MITGLIEDER_SPALTEN.length; i++) {
                Cell zelle = kopfzeile.createCell(i);
                zelle.setCellValue(MITGLIEDER_SPALTEN[i]);
                zelle.setCellStyle(kopfStil);
            }

            int zeilenIndex = 1;
            for (Mitglied m : mitglieder) {
                Row zeile = sheet.createRow(zeilenIndex++);
                zeile.createCell(0).setCellValue(wert(m.getVorname()));
                zeile.createCell(1).setCellValue(wert(m.getNachname()));
                zeile.createCell(2).setCellValue(wert(m.getStrasse()));
                zeile.createCell(3).setCellValue(wert(m.getPlz()));
                zeile.createCell(4).setCellValue(wert(m.getOrt()));
                zeile.createCell(5).setCellValue(wert(m.getEmail()));
                zeile.createCell(6).setCellValue(m.getEintrittsdatum() != null ? m.getEintrittsdatum().format(DATUM_FORMAT) : "");
                zeile.createCell(7).setCellValue(m.getAustrittsdatum() != null ? m.getAustrittsdatum().format(DATUM_FORMAT) : "");
                zeile.createCell(8).setCellValue(m.getStatus().name());
            }

            for (int i = 0; i < MITGLIEDER_SPALTEN.length; i++) {
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
