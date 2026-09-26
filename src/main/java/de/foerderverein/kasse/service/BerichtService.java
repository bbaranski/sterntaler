package de.foerderverein.kasse.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
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

import de.foerderverein.kasse.domain.Anrede;
import de.foerderverein.kasse.domain.Bereich;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;

/** Erzeugt die Basis-Berichte fuer die jaehrliche Kassenpruefung: Kassenbericht als PDF und
 * Mitgliederliste als Excel-Datei. Bewusst schlicht gehalten (Tabellen, keine grafische
 * Auswertung) - fuer den vorgesehenen Zweck (Ausdruck/Ablage, Vorlage beim Kassenpruefer,
 * Weiterverarbeitung in Excel) ausreichend. */
public class BerichtService {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private final BuchungRepository buchungRepository;
    private final KategorieRepository kategorieRepository;
    private final MitgliedService mitgliedService;

    public BerichtService(BuchungRepository buchungRepository, KategorieRepository kategorieRepository,
                           MitgliedService mitgliedService) {
        this.buchungRepository = buchungRepository;
        this.kategorieRepository = kategorieRepository;
        this.mitgliedService = mitgliedService;
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

    /** Jaehrliche "Ergaenzung zur Steuererklaerung": Gegenueberstellung von Einnahmen und
     * Ausgaben, klassifiziert nach den vier steuerlichen Sphaeren (Bereich) der jeweiligen
     * Kategorie (siehe Kategorie.getBereich()) - Aktivitaeten/Veranstaltungen spielen hier keine
     * Rolle mehr, sie sind reines optionales Reporting-Metadatum. Anders als der Kassenbericht
     * ohne Start-/Endsaldo-Konzept - reine Kalenderjahresbetrachtung fuer das Finanzamt, daher
     * eigene Jahresangabe statt eines (moeglicherweise nicht kalenderjahrgleichen)
     * Geschaeftsjahres. */
    public void erstelleSteuerreport(Path ziel, Konto konto, int jahr) throws IOException {
        SteuerreportDaten daten = berechneSteuerreport(konto, jahr);

        long einnahmenIdeell = summe(daten.einnahmenProKategorieIdeell());
        long ausgabenIdeell = summe(daten.ausgabenProKategorieIdeell());
        long einnahmenVermoegen = summe(daten.einnahmenProKategorieVermoegen());
        long ausgabenVermoegen = summe(daten.ausgabenProKategorieVermoegen());
        long einnahmenZweckbetrieb = summe(daten.einnahmenProKategorieZweckbetrieb());
        long ausgabenZweckbetrieb = summe(daten.ausgabenProKategorieZweckbetrieb());
        long einnahmenGeschaeftsbetrieb = summe(daten.einnahmenProKategorieGeschaeftsbetrieb());
        long ausgabenGeschaeftsbetrieb = summe(daten.ausgabenProKategorieGeschaeftsbetrieb());

        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        try (FileOutputStream out = new FileOutputStream(ziel.toFile())) {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titel = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font untertitel = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font abschnitt = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("Ergänzung zur Steuererklärung " + jahr, titel));
            document.add(new Paragraph(konto.getBezeichnung() + " (" + konto.getIban() + ")", untertitel));
            document.add(new Paragraph("Zeitraum: 01.01." + jahr + " - 31.12." + jahr, untertitel));
            document.add(leerzeile());

            document.add(new Paragraph("A. Ideeller Tätigkeitsbereich", abschnitt));
            document.add(new Paragraph("Einnahmen", untertitel));
            document.add(kategorieTabelle(daten.einnahmenProKategorieIdeell(), einnahmenIdeell));
            document.add(leerzeile());
            document.add(new Paragraph("Ausgaben", untertitel));
            document.add(kategorieTabelle(daten.ausgabenProKategorieIdeell(), ausgabenIdeell));
            document.add(leerzeile());
            document.add(ueberschussVerlustZeile("Überschuss", einnahmenIdeell, ausgabenIdeell, abschnitt));
            document.add(leerzeile());

            document.add(new Paragraph("B. Vermögensverwaltung", abschnitt));
            if (daten.einnahmenProKategorieVermoegen().isEmpty() && daten.ausgabenProKategorieVermoegen().isEmpty()) {
                document.add(new Paragraph("Keine", untertitel));
            } else {
                document.add(new Paragraph("Einnahmen", untertitel));
                document.add(kategorieTabelle(daten.einnahmenProKategorieVermoegen(), einnahmenVermoegen));
                document.add(leerzeile());
                document.add(new Paragraph("Ausgaben", untertitel));
                document.add(kategorieTabelle(daten.ausgabenProKategorieVermoegen(), ausgabenVermoegen));
                document.add(leerzeile());
                document.add(ueberschussVerlustZeile("Überschuss", einnahmenVermoegen, ausgabenVermoegen, abschnitt));
            }
            document.add(leerzeile());

            document.add(new Paragraph("C. Zweckbetriebe", abschnitt));
            if (daten.einnahmenProKategorieZweckbetrieb().isEmpty() && daten.ausgabenProKategorieZweckbetrieb().isEmpty()) {
                document.add(new Paragraph("Keine", untertitel));
            } else {
                document.add(new Paragraph("Einnahmen", untertitel));
                document.add(kategorieTabelle(daten.einnahmenProKategorieZweckbetrieb(), einnahmenZweckbetrieb));
                document.add(leerzeile());
                document.add(new Paragraph("Ausgaben", untertitel));
                document.add(kategorieTabelle(daten.ausgabenProKategorieZweckbetrieb(), ausgabenZweckbetrieb));
                document.add(leerzeile());
                document.add(ueberschussVerlustZeile("Überschuss", einnahmenZweckbetrieb, ausgabenZweckbetrieb, abschnitt));
            }
            document.add(leerzeile());

            document.add(new Paragraph("D. Wirtschaftlicher Geschäftsbetrieb", abschnitt));
            if (daten.einnahmenProKategorieGeschaeftsbetrieb().isEmpty() && daten.ausgabenProKategorieGeschaeftsbetrieb().isEmpty()) {
                document.add(new Paragraph("Keine", untertitel));
            } else {
                document.add(new Paragraph("Einnahmen", untertitel));
                document.add(kategorieTabelle(daten.einnahmenProKategorieGeschaeftsbetrieb(), einnahmenGeschaeftsbetrieb));
                document.add(leerzeile());
                document.add(new Paragraph("Ausgaben", untertitel));
                document.add(kategorieTabelle(daten.ausgabenProKategorieGeschaeftsbetrieb(), ausgabenGeschaeftsbetrieb));
                document.add(leerzeile());
                document.add(ueberschussVerlustZeile("Gewinn", einnahmenGeschaeftsbetrieb, ausgabenGeschaeftsbetrieb, abschnitt));
            }
            document.add(leerzeile());

            document.add(new Paragraph("Gesamtübersicht aller Bereiche", abschnitt));
            document.add(gesamtuebersichtTabelle(
                    einnahmenIdeell - ausgabenIdeell,
                    einnahmenVermoegen - ausgabenVermoegen,
                    einnahmenZweckbetrieb - ausgabenZweckbetrieb,
                    einnahmenGeschaeftsbetrieb - ausgabenGeschaeftsbetrieb));
            document.add(leerzeile());
            document.add(leerzeile());

            document.add(new Paragraph("Erstellt am " + java.time.LocalDate.now().format(DATUM_FORMAT), untertitel));
            document.add(leerzeile());
            document.add(new Paragraph("_______________________________          _______________________________", untertitel));
            document.add(new Paragraph("Datum, Unterschrift Kassenwart/-in          Datum, Unterschrift Kassenpruefer/-in", untertitel));

            document.close();
        } catch (DocumentException e) {
            throw new IOException("Steuerreport konnte nicht erzeugt werden", e);
        }
    }

    /** Reine Berechnung ohne PDF-Abhaengigkeit, damit dieser Teil ohne PDF-Textextraktion testbar
     * ist - siehe SteuerreportDaten. */
    SteuerreportDaten berechneSteuerreport(Konto konto, int jahr) {
        Map<Long, Kategorie> kategorienById = new LinkedHashMap<>();
        for (Kategorie k : kategorieRepository.findAll()) {
            kategorienById.put(k.getId(), k);
        }

        java.time.LocalDate start = java.time.LocalDate.of(jahr, 1, 1);
        java.time.LocalDate ende = java.time.LocalDate.of(jahr, 12, 31);

        List<Buchung> buchungen = buchungRepository.findAll().stream()
                .filter(b -> b.getKontoId().equals(konto.getId()))
                .filter(b -> !b.isStorniert())
                .filter(b -> !b.getBuchungsdatum().isBefore(start) && !b.getBuchungsdatum().isAfter(ende))
                .sorted(Comparator.comparing(Buchung::getBuchungsdatum))
                .toList();

        Map<String, Long> einnahmenIdeell = new LinkedHashMap<>();
        Map<String, Long> ausgabenIdeell = new LinkedHashMap<>();
        Map<String, Long> einnahmenVermoegen = new LinkedHashMap<>();
        Map<String, Long> ausgabenVermoegen = new LinkedHashMap<>();
        Map<String, Long> einnahmenZweckbetrieb = new LinkedHashMap<>();
        Map<String, Long> ausgabenZweckbetrieb = new LinkedHashMap<>();
        Map<String, Long> einnahmenGeschaeftsbetrieb = new LinkedHashMap<>();
        Map<String, Long> ausgabenGeschaeftsbetrieb = new LinkedHashMap<>();

        for (Buchung b : buchungen) {
            Kategorie k = kategorienById.get(b.getKategorieId());
            String kategorieName = k != null ? k.getName() : "(ohne Kategorie)";
            // Die steuerliche Sphaere haengt an der Kategorie (siehe Kategorie.getBereich()) -
            // Aktivitaeten/Veranstaltungen spielen hier keine Rolle mehr. Ohne zugeordnete
            // Kategorie gilt der Ideeller Bereich als Standardfall.
            Bereich bereich = k != null ? k.getBereich() : Bereich.IDEELLER_BEREICH;
            boolean istEinnahme = b.getBetragCent() >= 0;
            long betrag = Math.abs(b.getBetragCent());

            switch (bereich) {
                case IDEELLER_BEREICH -> {
                    if (istEinnahme) {
                        einnahmenIdeell.merge(kategorieName, betrag, Long::sum);
                    } else {
                        ausgabenIdeell.merge(kategorieName, betrag, Long::sum);
                    }
                }
                case VERMOEGENSVERWALTUNG -> {
                    if (istEinnahme) {
                        einnahmenVermoegen.merge(kategorieName, betrag, Long::sum);
                    } else {
                        ausgabenVermoegen.merge(kategorieName, betrag, Long::sum);
                    }
                }
                case ZWECKBETRIEB -> {
                    if (istEinnahme) {
                        einnahmenZweckbetrieb.merge(kategorieName, betrag, Long::sum);
                    } else {
                        ausgabenZweckbetrieb.merge(kategorieName, betrag, Long::sum);
                    }
                }
                case WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB -> {
                    if (istEinnahme) {
                        einnahmenGeschaeftsbetrieb.merge(kategorieName, betrag, Long::sum);
                    } else {
                        ausgabenGeschaeftsbetrieb.merge(kategorieName, betrag, Long::sum);
                    }
                }
            }
        }

        return new SteuerreportDaten(einnahmenIdeell, ausgabenIdeell, einnahmenVermoegen, ausgabenVermoegen,
                einnahmenZweckbetrieb, ausgabenZweckbetrieb, einnahmenGeschaeftsbetrieb, ausgabenGeschaeftsbetrieb);
    }

    private long summe(Map<String, Long> summenProKategorie) {
        return summenProKategorie.values().stream().mapToLong(Long::longValue).sum();
    }

    private Paragraph ueberschussVerlustZeile(String positivLabel, long einnahmenCent, long ausgabenCent, Font font) {
        long saldo = einnahmenCent - ausgabenCent;
        String label = saldo >= 0 ? positivLabel : "Verlust";
        return new Paragraph(label + ": " + BETRAG_FORMAT.format(BigDecimal.valueOf(Math.abs(saldo), 2)), font);
    }

    private PdfPTable gesamtuebersichtTabelle(long netA, long netB, long netC, long netD) {
        PdfPTable tabelle = new PdfPTable(4);
        tabelle.setWidthPercentage(100);
        tabelle.addCell(kopfzelle("A"));
        tabelle.addCell(kopfzelle("B"));
        tabelle.addCell(kopfzelle("C"));
        tabelle.addCell(kopfzelle("D"));
        tabelle.addCell(betragZelle(netA));
        tabelle.addCell(betragZelle(netB));
        tabelle.addCell(betragZelle(netC));
        tabelle.addCell(betragZelle(netD));
        return tabelle;
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
            "Id", "Anrede", "Vorname", "Nachname", "Straße", "PLZ", "Ort", "E-Mail", "Eintrittsdatum",
            "Austrittsdatum", "Status", "Jahresbeitrag", "IBAN", "BIC", "Mandatsreferenz", "Sammellastschrift", "Notiz"
    };

    /** Excel-Datei (.xlsx) mit allen Mitgliedern - fuer Weitergabe/Weiterverarbeitung ausserhalb
     * der Anwendung (z. B. Serienbriefe, Abgleich mit anderen Listen). */
    public void exportiereMitgliederliste(Path ziel) throws IOException {
        List<Mitglied> mitglieder = mitgliedService.findAll();
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
                zeile.createCell(0).setCellValue(m.getId() != null ? String.valueOf(m.getId()) : "");
                zeile.createCell(1).setCellValue(m.getAnrede() != null ? m.getAnrede().name() : "");
                zeile.createCell(2).setCellValue(wert(m.getVorname()));
                zeile.createCell(3).setCellValue(wert(m.getNachname()));
                zeile.createCell(4).setCellValue(wert(m.getStrasse()));
                zeile.createCell(5).setCellValue(wert(m.getPlz()));
                zeile.createCell(6).setCellValue(wert(m.getOrt()));
                zeile.createCell(7).setCellValue(wert(m.getEmail()));
                zeile.createCell(8).setCellValue(wert(m.getEintrittsdatum()));
                zeile.createCell(9).setCellValue(wert(m.getAustrittsdatum()));
                zeile.createCell(10).setCellValue(m.getStatus().name());
                zeile.createCell(11).setCellValue(m.getJahresbeitragEuro() != null
                        ? m.getJahresbeitragEuro().toPlainString().replace('.', ',') : "");
                zeile.createCell(12).setCellValue(wert(m.getIban()));
                zeile.createCell(13).setCellValue(wert(m.getBic()));
                zeile.createCell(14).setCellValue(m.getMandatsreferenz() != null
                        ? String.valueOf(m.getMandatsreferenz()) : "");
                zeile.createCell(15).setCellValue(wert(m.getSammellastschrift()));
                zeile.createCell(16).setCellValue(wert(m.getNotiz()));
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

    /** Liest eine Excel-Datei (.xlsx) im exakt gleichen Format wie exportiereMitgliederliste
     * (gleiche Spaltenreihenfolge, gleiche Kopfzeile) und legt je Datenzeile ein neues Mitglied
     * an - ueber MitgliedService.save(...), damit dieselbe Validierung/Normalisierung greift wie
     * bei manueller Erfassung. Fehlerhafte Zeilen (z. B. fehlender Nachname) werden uebersprungen
     * und gesammelt zurueckgegeben, statt den gesamten Import abzubrechen. */
    public MitgliederImportErgebnis importiereMitgliederliste(Path quelle) throws IOException {
        try (FileInputStream in = new FileInputStream(quelle.toFile());
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            pruefeKopfzeile(sheet);

            DataFormatter formatter = new DataFormatter();
            int importiert = 0;
            List<String> fehler = new ArrayList<>();
            for (int zeilenIndex = 1; zeilenIndex <= sheet.getLastRowNum(); zeilenIndex++) {
                Row zeile = sheet.getRow(zeilenIndex);
                if (zeile == null
                        || (zellenwert(formatter, zeile, 2).isBlank() && zellenwert(formatter, zeile, 3).isBlank())) {
                    continue;
                }
                try {
                    mitgliedService.save(mitgliedAusZeile(formatter, zeile));
                    importiert++;
                } catch (Exception e) {
                    fehler.add("Zeile " + (zeilenIndex + 1) + ": " + e.getMessage());
                }
            }
            return new MitgliederImportErgebnis(importiert, fehler);
        }
    }

    private void pruefeKopfzeile(Sheet sheet) throws IOException {
        Row kopfzeile = sheet.getRow(0);
        DataFormatter formatter = new DataFormatter();
        if (kopfzeile == null || !MITGLIEDER_SPALTEN[0].equals(zellenwert(formatter, kopfzeile, 0))) {
            throw new IOException("Datei entspricht nicht dem erwarteten Format der Mitgliederliste "
                    + "(Kopfzeile fehlt oder passt nicht) - bitte eine zuvor exportierte Mitgliederliste verwenden.");
        }
    }

    private Mitglied mitgliedAusZeile(DataFormatter formatter, Row zeile) {
        // Spalte 0 (Id) wird bewusst nicht gelesen - jede importierte Zeile legt ein neues
        // Mitglied mit neu vergebener Id an, siehe Klassenkommentar von importiereMitgliederliste.
        Mitglied mitglied = new Mitglied();
        String anredeText = zellenwert(formatter, zeile, 1);
        mitglied.setAnrede(anredeText.isBlank() ? null : Anrede.valueOf(anredeText.trim().toUpperCase(Locale.ROOT)));
        mitglied.setVorname(zellenwert(formatter, zeile, 2));
        mitglied.setNachname(zellenwert(formatter, zeile, 3));
        mitglied.setStrasse(leerZuNull(zellenwert(formatter, zeile, 4)));
        mitglied.setPlz(leerZuNull(zellenwert(formatter, zeile, 5)));
        mitglied.setOrt(leerZuNull(zellenwert(formatter, zeile, 6)));
        mitglied.setEmail(leerZuNull(zellenwert(formatter, zeile, 7)));
        mitglied.setEintrittsdatum(leerZuNull(zellenwert(formatter, zeile, 8)));
        mitglied.setAustrittsdatum(leerZuNull(zellenwert(formatter, zeile, 9)));
        String statusText = zellenwert(formatter, zeile, 10);
        mitglied.setStatus(statusText.isBlank() ? MitgliedStatus.AKTIV : MitgliedStatus.valueOf(statusText.trim().toUpperCase(Locale.ROOT)));
        String jahresbeitragText = zellenwert(formatter, zeile, 11);
        mitglied.setJahresbeitragEuro(jahresbeitragText.isBlank()
                ? null : new BigDecimal(jahresbeitragText.replace(',', '.')));
        mitglied.setIban(leerZuNull(zellenwert(formatter, zeile, 12)));
        mitglied.setBic(leerZuNull(zellenwert(formatter, zeile, 13)));
        String mandatsreferenzText = zellenwert(formatter, zeile, 14);
        try {
            mitglied.setMandatsreferenz(mandatsreferenzText.isBlank() ? null : Long.parseLong(mandatsreferenzText));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mandatsreferenz muss eine Ganzzahl sein");
        }
        mitglied.setSammellastschrift(leerZuNull(zellenwert(formatter, zeile, 15)));
        mitglied.setNotiz(leerZuNull(zellenwert(formatter, zeile, 16)));
        return mitglied;
    }

    private String leerZuNull(String text) {
        return text.isBlank() ? null : text.trim();
    }

    private String zellenwert(DataFormatter formatter, Row zeile, int spalte) {
        Cell zelle = zeile.getCell(spalte);
        return zelle == null ? "" : formatter.formatCellValue(zelle).trim();
    }
}
