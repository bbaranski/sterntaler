package de.foerderverein.kasse.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import org.openpdf.text.Rectangle;
import org.openpdf.text.Paragraph;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Anrede;
import de.foerderverein.kasse.domain.Bereich;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;
import de.foerderverein.kasse.domain.Verein;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.VereinRepository;

/** Erzeugt die Basis-Berichte fuer die jaehrliche Kassenpruefung: Kassenbericht als PDF und
 * Mitgliederliste als Excel-Datei. Bewusst schlicht gehalten (Tabellen, keine grafische
 * Auswertung) - fuer den vorgesehenen Zweck (Ausdruck/Ablage, Vorlage beim Kassenpruefer,
 * Weiterverarbeitung in Excel) ausreichend. */
public class BerichtService {

    /** Sammelposten fuer Buchungen ohne Zuordnung - eine Schreibweise fuer alle Berichte und
     * zugleich die der Auswahlliste (siehe KategorieAuswahl/AktivitaetAuswahl). */
    static final String OHNE_KATEGORIE = "(ohne Kategorie)";
    static final String OHNE_AKTIVITAET = "(ohne Aktivität)";

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private final BuchungRepository buchungRepository;
    private final KategorieRepository kategorieRepository;
    private final AktivitaetRepository aktivitaetRepository;
    private final MitgliedService mitgliedService;
    private final GeschaeftsjahrService geschaeftsjahrService;
    private final VereinRepository vereinRepository;

    public BerichtService(BuchungRepository buchungRepository, KategorieRepository kategorieRepository,
                           AktivitaetRepository aktivitaetRepository, MitgliedService mitgliedService,
                           GeschaeftsjahrService geschaeftsjahrService, VereinRepository vereinRepository) {
        this.buchungRepository = buchungRepository;
        this.kategorieRepository = kategorieRepository;
        this.aktivitaetRepository = aktivitaetRepository;
        this.mitgliedService = mitgliedService;
        this.geschaeftsjahrService = geschaeftsjahrService;
        this.vereinRepository = vereinRepository;
    }

    public void erstelleKassenbericht(Path ziel, Konto konto, Geschaeftsjahr jahr) throws IOException {
        Map<Long, Kategorie> kategorienById = new LinkedHashMap<>();
        for (Kategorie k : kategorieRepository.findAll()) {
            kategorienById.put(k.getId(), k);
        }

        List<Buchung> buchungen = buchungenFuer(konto, jahr);

        Map<String, Long> summeProKategorieEinnahme = new LinkedHashMap<>();
        Map<String, Long> summeProKategorieAusgabe = new LinkedHashMap<>();
        long summeEinnahmen = 0;
        long summeAusgaben = 0;
        for (Buchung b : buchungen) {
            Kategorie k = kategorienById.get(b.getKategorieId());
            String name = k != null ? k.getName() : OHNE_KATEGORIE;
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
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Seitenzahlen());
            document.open();

            Font titel = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font untertitel = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font abschnitt = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font saldoZeile = betragFett(11);
            Font saldoAbschluss = betragFett(12);

            document.add(new Paragraph("Kassenbericht " + jahr.getBezeichnung(), titel));
            for (Paragraph zeile : vereinKopf(abschnitt, untertitel)) {
                document.add(zeile);
            }
            document.add(new Paragraph(konto.getBezeichnung() + " (" + konto.getIban() + ")", untertitel));
            document.add(new Paragraph("Zeitraum: " + jahr.getStartDatum().format(DATUM_FORMAT)
                    + " - " + jahr.getEndDatum().format(DATUM_FORMAT), untertitel));
            document.add(new Paragraph("Startsaldo: " + BETRAG_FORMAT.format(jahr.getStartsaldoEuro()), saldoZeile));
            document.add(leerzeile());

            document.add(new Paragraph("Einnahmen", abschnitt));
            document.add(kategorieTabelle(summeProKategorieEinnahme, summeEinnahmen));
            document.add(leerzeile());

            document.add(new Paragraph("Ausgaben", abschnitt));
            document.add(kategorieTabelle(summeProKategorieAusgabe, summeAusgaben));
            document.add(leerzeile());

            long saldoZeitraum = summeEinnahmen - summeAusgaben;
            long endsaldo = jahr.getStartsaldoCent() + saldoZeitraum;
            document.add(new Paragraph("Saldo des Zeitraums: " + BETRAG_FORMAT.format(BigDecimal.valueOf(saldoZeitraum, 2)), saldoAbschluss));
            document.add(new Paragraph("Endsaldo: " + BETRAG_FORMAT.format(BigDecimal.valueOf(endsaldo, 2)), saldoAbschluss));
            document.add(leerzeile());
            document.add(leerzeile());

            document.add(new Paragraph("Erstellt am " + LocalDate.now().format(DATUM_FORMAT), untertitel));
            document.add(leerzeile());
            document.add(new Paragraph("_______________________________          _______________________________", untertitel));
            document.add(new Paragraph("Datum, Unterschrift Kassenwart/-in          Datum, Unterschrift Kassenprüfer/-in", untertitel));

            // Muss vor dem Schliessen des OutputStreams durch try-with-resources passieren:
            // document.close() schreibt noch Trailer/Xref-Tabelle in den Stream.
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Kassenbericht konnte nicht erzeugt werden", e);
        }
    }

    /** Alle Buchungen eines Geschaeftsjahres einzeln, als Gegenstueck zu den
     * Summenberichten. Im Querformat, weil acht Spalten im Hochformat nicht nebeneinander
     * passen. Buchungsmenge und Endsaldo entsprechen dem Kassenbericht. Erscheint in der
     * Oberflaeche und im PDF-Titel als "Kassenpruefbericht" - er geht mit Unterschriftenzeile
     * und Entlastungsempfehlung an die Mitgliederversammlung; im Code heisst er weiter
     * Buchungsjournal. */
    public void erstelleBuchungsjournal(Path ziel, Konto konto, Geschaeftsjahr jahr) throws IOException {
        BuchungsjournalDaten daten = berechneBuchungsjournal(konto, jahr);

        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        // Bewusst nicht PageSize.A4.rotate(): das schreibt eine Hochformat-MediaBox mit
        // /Rotate 90 - Betrachter drehen sie, Werkzeuge die /Rotate ignorieren aber nicht. Eine
        // direkt im Querformat angelegte Seite ist eindeutig.
        Document document = new Document(
                new Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth()), 40, 40, 50, 50);
        try (FileOutputStream out = new FileOutputStream(ziel.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Seitenzahlen());
            document.open();

            Font titel = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font untertitel = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font abschnitt = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font saldoZeile = betragFett(11);
            Font saldoAbschluss = betragFett(12);

            document.add(new Paragraph("Kassenprüfbericht " + jahr.getBezeichnung(), titel));
            for (Paragraph zeile : vereinKopf(abschnitt, untertitel)) {
                document.add(zeile);
            }
            document.add(new Paragraph(konto.getBezeichnung() + " (" + konto.getIban() + ")", untertitel));
            document.add(new Paragraph("Zeitraum: " + jahr.getStartDatum().format(DATUM_FORMAT)
                    + " - " + jahr.getEndDatum().format(DATUM_FORMAT), untertitel));
            document.add(new Paragraph("Startsaldo: "
                    + BETRAG_FORMAT.format(BigDecimal.valueOf(daten.startsaldoCent(), 2)), saldoZeile));
            document.add(leerzeile());

            document.add(journalTabelle(daten.zeilen()));
            document.add(leerzeile());
            document.add(new Paragraph("Summe der Beträge: "
                    + BETRAG_FORMAT.format(BigDecimal.valueOf(daten.summeCent(), 2)), saldoAbschluss));
            document.add(new Paragraph("Endsaldo: "
                    + BETRAG_FORMAT.format(BigDecimal.valueOf(daten.endsaldoCent(), 2)), saldoAbschluss));
            document.add(leerzeile());

            document.add(new Paragraph("Die Kassenprüfung hat keine Beanstandungen ergeben."
                    + " Die Kassenprüfer/-innen empfehlen der Mitgliederversammlung die Entlastung"
                    + " des Vorstands.", untertitel));
            document.add(leerzeile());
            document.add(leerzeile());

            document.add(new Paragraph("Erstellt am " + LocalDate.now().format(DATUM_FORMAT), untertitel));
            document.add(leerzeile());
            document.add(new Paragraph("_______________________________          _______________________________", untertitel));
            document.add(new Paragraph("Datum, Unterschrift Kassenwart/-in          Datum, Unterschrift Kassenprüfer/-in", untertitel));

            // Muss vor dem Schliessen des OutputStreams durch try-with-resources passieren:
            // document.close() schreibt noch Trailer/Xref-Tabelle in den Stream.
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Kassenprüfbericht konnte nicht erzeugt werden", e);
        }
    }

    /** Getrennt von der PDF-Erzeugung, damit die Zahlen pruefbar sind - wie bei
     * berechneSteuerreport(...). Der laufende Saldo kommt aus GeschaeftsjahrService, damit
     * Journal und Buchungsuebersicht nie unterschiedliche Werte zeigen. */
    BuchungsjournalDaten berechneBuchungsjournal(Konto konto, Geschaeftsjahr jahr) {
        Map<Long, Kategorie> kategorienById = new LinkedHashMap<>();
        for (Kategorie k : kategorieRepository.findAll()) {
            kategorienById.put(k.getId(), k);
        }
        Map<Long, Aktivitaet> aktivitaetenById = new LinkedHashMap<>();
        for (Aktivitaet a : aktivitaetRepository.findAll()) {
            aktivitaetenById.put(a.getId(), a);
        }

        List<Buchung> buchungen = buchungenFuer(konto, jahr);
        // Nur das eine Jahr uebergeben: berechneLaufendeSalden gruppiert nach (Konto,
        // Geschaeftsjahr), und buchungenFuer(...) hat die Zugehoerigkeit schon sichergestellt.
        Map<Long, Long> laufendeSalden =
                geschaeftsjahrService.berechneLaufendeSalden(buchungen, List.of(jahr));

        List<BuchungsjournalDaten.Zeile> zeilen = new ArrayList<>();
        long summe = 0;
        for (Buchung b : buchungen) {
            Kategorie k = kategorienById.get(b.getKategorieId());
            Aktivitaet a = aktivitaetenById.get(b.getAktivitaetId());
            summe += b.getBetragCent();
            zeilen.add(new BuchungsjournalDaten.Zeile(
                    b.getBuchungsdatum(),
                    wert(b.getKontoauszug()),
                    wert(b.getRechnungsnummer()),
                    b.getBetragCent(),
                    laufendeSalden.getOrDefault(b.getId(), 0L),
                    k != null ? k.getName() : "",
                    a != null ? a.getName() : "",
                    wert(b.getVerwendungszweck())));
        }
        return new BuchungsjournalDaten(zeilen, jahr.getStartsaldoCent(), summe,
                jahr.getStartsaldoCent() + summe);
    }

    /** Acht Spalten im Querformat. Die Zellhelfer der uebrigen Berichte sind auf Hochformat
     * ausgelegt (10 pt fette Kopfzeile, 12 pt Inhalt) - hier zu gross, daher durchgaengig 8 pt. */
    private PdfPTable journalTabelle(List<BuchungsjournalDaten.Zeile> zeilen) {
        Font klein = new Font(Font.HELVETICA, 8, Font.NORMAL);
        Font kleinFett = new Font(Font.HELVETICA, 8, Font.BOLD);
        PdfPTable tabelle = new PdfPTable(8);
        tabelle.setWidthPercentage(100);
        try {
            // Breiten in Punkt bei A4-Querformat und 40 pt Rand (Summe 762). "Kontoauszug"
            // braucht als Kopfzeile bei 8 pt fett rund 54 pt, sonst bricht die Beschriftung um.
            tabelle.setWidths(new float[] {58, 64, 52, 68, 72, 105, 90, 253});
        } catch (DocumentException e) {
            // Feste Spaltenzahl, kann hier nicht auftreten.
        }
        for (String spalte : new String[] {"Datum", "Kontoauszug", "Rechnung"}) {
            tabelle.addCell(kopfzelle(spalte, kleinFett));
        }
        for (String spalte : new String[] {"Betrag", "Saldo"}) {
            PdfPCell kopf = kopfzelle(spalte, kleinFett);
            kopf.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tabelle.addCell(kopf);
        }
        for (String spalte : new String[] {"Kategorie", "Aktivität", "Verwendungszweck"}) {
            tabelle.addCell(kopfzelle(spalte, kleinFett));
        }
        // Das Journal wird mehrseitig: ohne Kopfzeilenwiederholung stehen die Zeilen der
        // Folgeseiten ohne Spaltenbeschriftung.
        tabelle.setHeaderRows(1);

        if (zeilen.isEmpty()) {
            PdfPCell leer = new PdfPCell(new org.openpdf.text.Phrase("(keine Buchungen)", klein));
            leer.setColspan(8);
            tabelle.addCell(leer);
        }
        for (BuchungsjournalDaten.Zeile zeile : zeilen) {
            tabelle.addCell(textZelle(zeile.datum().format(DATUM_FORMAT), klein));
            tabelle.addCell(textZelle(zeile.kontoauszug(), klein));
            tabelle.addCell(textZelle(zeile.rechnungsnummer(), klein));
            tabelle.addCell(betragZelle(zeile.betragCent(), klein));
            tabelle.addCell(betragZelle(zeile.saldoCent(), klein));
            tabelle.addCell(textZelle(zeile.kategorie(), klein));
            tabelle.addCell(textZelle(zeile.aktivitaet(), klein));
            tabelle.addCell(textZelle(zeile.verwendungszweck(), klein));
        }
        return tabelle;
    }

    private PdfPCell textZelle(String text, Font font) {
        return new PdfPCell(new org.openpdf.text.Phrase(text, font));
    }

    /** Jaehrliche "Einnahmen-Ueberschuss-Rechnung": Gegenueberstellung von Einnahmen und
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
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Seitenzahlen());
            document.open();

            Font titel = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font untertitel = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font abschnitt = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("Einnahmen-Überschuss-Rechnung " + jahr, titel));
            for (Paragraph zeile : vereinKopf(abschnitt, untertitel)) {
                document.add(zeile);
            }
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

            document.add(new Paragraph("Erstellt am " + LocalDate.now().format(DATUM_FORMAT), untertitel));
            document.add(leerzeile());
            document.add(new Paragraph("_______________________________          _______________________________", untertitel));
            document.add(new Paragraph("Datum, Unterschrift Kassenwart/-in          Datum, Unterschrift Kassenprüfer/-in", untertitel));

            document.close();
        } catch (DocumentException e) {
            throw new IOException("Einnahmen-Überschuss-Rechnung konnte nicht erzeugt werden", e);
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
                .filter(BerichtService::hatBetrag)
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
            String kategorieName = k != null ? k.getName() : OHNE_KATEGORIE;
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

    /** Kennzahlen zu den Mitgliedern als PDF, im Aufbau wie der Kassenbericht. Anders als dieser
     * ohne Zeitraum: Mitgliederdaten tragen keinen Geschaeftsjahrbezug, der Bericht ist eine
     * Momentaufnahme zum Erstellungstag. Inhalt sind die Kennzahlen der Statuszeile der
     * Mitgliederuebersicht, dort je nach Filter nur fuer eine Gruppe - hier nebeneinander fuer
     * Aktive, Inaktive und den Gesamtbestand. */
    public void erstelleMitgliederbericht(Path ziel) throws IOException {
        MitgliederberichtDaten daten = berechneMitgliederbericht();

        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        try (FileOutputStream out = new FileOutputStream(ziel.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Seitenzahlen());
            document.open();

            Font titel = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font untertitel = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font abschnitt = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("Mitgliederentwicklung", titel));
            for (Paragraph zeile : vereinKopf(abschnitt, untertitel)) {
                document.add(zeile);
            }
            document.add(new Paragraph("Stand: " + LocalDate.now().format(DATUM_FORMAT), untertitel));
            document.add(leerzeile());

            document.add(new Paragraph("Mitglieder und Jahresbeitrag", abschnitt));
            document.add(kennzahlenTabelle(daten));

            // Muss vor dem Schliessen des OutputStreams durch try-with-resources passieren:
            // document.close() schreibt noch Trailer/Xref-Tabelle in den Stream.
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Mitgliederentwicklung konnte nicht erzeugt werden", e);
        }
    }

    /** Getrennt von der PDF-Erzeugung, damit die Kennzahlen pruefbar sind - wie bei
     * berechneSteuerreport(...). */
    /** Alle aktiven Mitglieder mit ihren Stammdaten, nach Id aufsteigend - das Gegenstueck zur
     * Mitgliederentwicklung, die nur Kennzahlen zeigt. Im Querformat und in kleiner Schrift, weil
     * die Spalten im Hochformat nicht nebeneinander passen. Kopfzeilen wie in der
     * Mitgliederentwicklung. */
    public void erstelleMitgliederuebersicht(Path ziel) throws IOException {
        List<Mitglied> mitglieder = mitgliedService.findAll().stream()
                .filter(m -> m.getStatus() == MitgliedStatus.AKTIV)
                .sorted(Comparator.comparing(Mitglied::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        // Querformat wie beim Kassenpruefbericht, und aus demselben Grund direkt als
        // Querformat-Seite statt per /Rotate (siehe erstelleBuchungsjournal). Seitlich schmalere
        // Raender als die uebrigen Berichte: 18 Spalten brauchen jeden Punkt, und bei 40 pt
        // brechen Orte und Notizen mitten im Wort um.
        Document document = new Document(
                new Rectangle(PageSize.A4.getHeight(), PageSize.A4.getWidth()), 25, 25, 50, 50);
        try (FileOutputStream out = new FileOutputStream(ziel.toFile())) {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new Seitenzahlen());
            document.open();

            Font titel = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font untertitel = new Font(Font.HELVETICA, 11, Font.NORMAL);
            Font abschnitt = new Font(Font.HELVETICA, 12, Font.BOLD);

            document.add(new Paragraph("Mitgliederübersicht", titel));
            for (Paragraph zeile : vereinKopf(abschnitt, untertitel)) {
                document.add(zeile);
            }
            document.add(new Paragraph("Stand: " + LocalDate.now().format(DATUM_FORMAT), untertitel));
            document.add(leerzeile());

            document.add(mitgliederTabelle(mitglieder));

            // Muss vor dem Schliessen des OutputStreams durch try-with-resources passieren:
            // document.close() schreibt noch Trailer/Xref-Tabelle in den Stream.
            document.close();
        } catch (DocumentException e) {
            throw new IOException("Mitgliederübersicht konnte nicht erzeugt werden", e);
        }
    }

    /** Spalten der Mitgliederuebersicht: die des Mitgliederexports (MITGLIEDER_SPALTEN) ohne
     * Austrittsdatum, Status und Sammellastschrift. Der Bericht zeigt nur aktive Mitglieder -
     * Austrittsdatum und Status haetten dort ohnehin in jeder Zeile denselben Inhalt. */
    private static final String[] MITGLIEDERUEBERSICHT_SPALTEN = {
            "Id", "Anrede", "Vorname", "Nachname", "Straße", "PLZ", "Ort", "Telefon", "E-Mail",
            "Eintrittsdatum", "Jahresbeitrag", "IBAN", "BIC", "Mandatsreferenz", "Notiz"
    };

    private PdfPTable mitgliederTabelle(List<Mitglied> mitglieder) {
        Font klein = new Font(Font.HELVETICA, 6, Font.NORMAL);
        Font kleinFett = new Font(Font.HELVETICA, 6, Font.BOLD);
        PdfPTable tabelle = new PdfPTable(MITGLIEDERUEBERSICHT_SPALTEN.length);
        tabelle.setWidthPercentage(100);
        try {
            // Breiten in Punkt bei A4-Querformat und 25 pt Rand (Summe 792), je Spalte 4 pt
            // Zellenrand eingerechnet. Untergrenze ist die Breite der Beschriftung bei 6 pt fett:
            // "Sammellastschrift" und "Mandatsreferenz" sind einzelne Woerter, die in einer zu
            // schmalen Spalte abgeschnitten wuerden statt umzubrechen. Darueber hinaus so
            // bemessen, dass die haeufigen einteiligen Werte - Ort, Telefon, IBAN, Datum - in
            // eine Zeile passen; mehrteilige (Doppelnamen, lange E-Mail-Adressen) brechen an
            // ihrer Trennstelle um.
            tabelle.setWidths(new float[] {
                    18, 27, 38, 62, 56, 21, 50, 52, 125, 45, 44, 80, 49, 52, 73});
        } catch (DocumentException e) {
            // Feste Spaltenzahl, kann hier nicht auftreten.
        }
        for (String spalte : MITGLIEDERUEBERSICHT_SPALTEN) {
            tabelle.addCell(kopfzelle(spalte, kleinFett));
        }
        // Die Liste wird mehrseitig: ohne Kopfzeilenwiederholung stehen die Zeilen der
        // Folgeseiten ohne Spaltenbeschriftung.
        tabelle.setHeaderRows(1);

        if (mitglieder.isEmpty()) {
            PdfPCell leer = new PdfPCell(new org.openpdf.text.Phrase("(keine aktiven Mitglieder)", klein));
            leer.setColspan(MITGLIEDERUEBERSICHT_SPALTEN.length);
            tabelle.addCell(leer);
        }
        // Spaltenreihenfolge und Schreibweise der Werte wie in exportiereMitgliederliste (nur
        // ohne die drei dort zusaetzlichen Spalten) - beide geben dieselben Stammdaten aus und
        // sollen nicht auseinanderlaufen.
        for (Mitglied m : mitglieder) {
            tabelle.addCell(textZelle(m.getId() != null ? String.valueOf(m.getId()) : "", klein));
            tabelle.addCell(textZelle(anredeText(m.getAnrede()), klein));
            tabelle.addCell(textZelle(wert(m.getVorname()), klein));
            tabelle.addCell(textZelle(wert(m.getNachname()), klein));
            tabelle.addCell(textZelle(wert(m.getStrasse()), klein));
            tabelle.addCell(textZelle(wert(m.getPlz()), klein));
            tabelle.addCell(textZelle(wert(m.getOrt()), klein));
            tabelle.addCell(textZelle(wert(m.getTelefon()), klein));
            tabelle.addCell(textZelle(wert(m.getEmail()), klein));
            tabelle.addCell(textZelle(wert(m.getEintrittsdatum()), klein));
            tabelle.addCell(m.getJahresbeitragCent() != null
                    ? betragZelle(m.getJahresbeitragCent(), klein)
                    : textZelle("", klein));
            tabelle.addCell(textZelle(wert(m.getIban()), klein));
            tabelle.addCell(textZelle(wert(m.getBic()), klein));
            tabelle.addCell(textZelle(m.getMandatsreferenz() != null
                    ? String.valueOf(m.getMandatsreferenz()) : "", klein));
            tabelle.addCell(textZelle(wert(m.getNotiz()), klein));
        }
        return tabelle;
    }

    private static String anredeText(Anrede anrede) {
        if (anrede == null) {
            return "";
        }
        return switch (anrede) {
            case HERR -> "Herr";
            case FRAU -> "Frau";
            case FAMILIE -> "Familie";
        };
    }

    MitgliederberichtDaten berechneMitgliederbericht() {
        List<Mitglied> aktive = mitgliedService.findAll().stream()
                .filter(m -> m.getStatus() == MitgliedStatus.AKTIV)
                .toList();
        List<Long> betraege = aktive.stream()
                .map(Mitglied::getJahresbeitragCent)
                .filter(Objects::nonNull)
                .toList();
        if (betraege.isEmpty()) {
            return new MitgliederberichtDaten(aktive.size(), 0, null, null, null);
        }
        long summe = betraege.stream().mapToLong(Long::longValue).sum();
        // Auf ganze Cent gerundet - gleichbedeutend mit dem auf zwei Nachkommastellen gerundeten
        // Eurobetrag der Statuszeile.
        long durchschnitt = BigDecimal.valueOf(summe)
                .divide(BigDecimal.valueOf(betraege.size()), 0, RoundingMode.HALF_UP).longValue();
        return new MitgliederberichtDaten(aktive.size(), summe,
                betraege.stream().min(Comparator.naturalOrder()).orElseThrow(),
                betraege.stream().max(Comparator.naturalOrder()).orElseThrow(),
                durchschnitt);
    }

    /** Zweispaltig wie die Tabellen des Kassenberichts (siehe kategorieTabelle). */
    private PdfPTable kennzahlenTabelle(MitgliederberichtDaten daten) {
        PdfPTable tabelle = new PdfPTable(2);
        tabelle.setWidthPercentage(100);
        try {
            tabelle.setWidths(new float[] {70, 30});
        } catch (DocumentException e) {
            // Feste Spaltenzahl, kann hier nicht auftreten.
        }
        tabelle.addCell(kopfzelle("Kennzahl"));
        PdfPCell wertKopf = kopfzelle("Aktiv");
        wertKopf.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tabelle.addCell(wertKopf);

        tabelle.addCell("Mitglieder");
        tabelle.addCell(anzahlZelle(daten.mitglieder()));
        // Der Strich steht fuer "kein aktives Mitglied hat einen Jahresbeitrag" - dieselbe
        // Bedingung, aus der auch minCent/maxCent/durchschnittCent leer bleiben.
        tabelle.addCell("Jahresbeitrag Summe");
        tabelle.addCell(daten.minCent() == null ? ohneWertZelle() : betragZelle(daten.summeCent()));
        tabelle.addCell("Jahresbeitrag Minimum");
        tabelle.addCell(betragOderOhneWert(daten.minCent()));
        tabelle.addCell("Jahresbeitrag Maximum");
        tabelle.addCell(betragOderOhneWert(daten.maxCent()));
        tabelle.addCell("Jahresbeitrag Durchschnitt");
        tabelle.addCell(betragOderOhneWert(daten.durchschnittCent()));
        return tabelle;
    }

    private PdfPCell anzahlZelle(int anzahl) {
        PdfPCell cell = new PdfPCell(new org.openpdf.text.Phrase(String.valueOf(anzahl)));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private PdfPCell betragOderOhneWert(Long cent) {
        return cent == null ? ohneWertZelle() : betragZelle(cent);
    }

    /** Kein aktives Mitglied hat einen Jahresbeitrag - 0,00 Euro waere hier eine falsche
     * Aussage, daher ein Strich. */
    private PdfPCell ohneWertZelle() {
        PdfPCell cell = new PdfPCell(new org.openpdf.text.Phrase("—"));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
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

    /** Fusszeile "Seite x von y" auf jeder Seite jedes Berichts. Die Gesamtzahl der Seiten steht
     * erst fest, wenn das Dokument geschrieben ist - jede Seite laesst dafuer ein PdfTemplate
     * frei, das beim Schliessen einmal mit der endgueltigen Zahl gefuellt wird und auf allen
     * Seiten zugleich erscheint. */
    private static final class Seitenzahlen extends PdfPageEventHelper {

        private static final float SCHRIFTGROESSE = 9;

        private BaseFont schrift;
        private PdfTemplate gesamtseiten;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                schrift = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (DocumentException | IOException e) {
                throw new IllegalStateException("Schrift fuer die Seitenzahlen nicht verfuegbar", e);
            }
            gesamtseiten = writer.getDirectContent().createTemplate(40, SCHRIFTGROESSE + 2);
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            String text = "Seite " + writer.getPageNumber() + " von ";
            float textBreite = schrift.getWidthPoint(text, SCHRIFTGROESSE);
            // Zentriert unter dem Satzspiegel, mit der Ziffernbreite der Gesamtzahl (grob zwei
            // Stellen) im Mittelpunkt - sonst wandert die Zeile mit wachsender Seitenzahl.
            float ziffernBreite = schrift.getWidthPoint("00", SCHRIFTGROESSE);
            float x = (document.left() + document.right() - textBreite - ziffernBreite) / 2;
            float y = document.bottom() - 20;

            PdfContentByte inhalt = writer.getDirectContent();
            inhalt.beginText();
            inhalt.setFontAndSize(schrift, SCHRIFTGROESSE);
            inhalt.setTextMatrix(x, y);
            inhalt.showText(text);
            inhalt.endText();
            inhalt.addTemplate(gesamtseiten, x + textBreite, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            // getPageNumber() steht hier bereits eine Seite hinter der letzten geschriebenen.
            gesamtseiten.beginText();
            gesamtseiten.setFontAndSize(schrift, SCHRIFTGROESSE);
            gesamtseiten.setTextMatrix(0, 0);
            gesamtseiten.showText(String.valueOf(writer.getPageNumber() - 1));
            gesamtseiten.endText();
        }
    }

    /** Fette Schrift fuer Zeilen, die einen Geldbetrag enthalten. Fuer Zeichen jenseits von
     * Latin-1 - hier das Euro-Zeichen - ersetzt PdfChunk eine Schrift ohne explizit gesetzte
     * BaseFont durch die in OpenPDF mitgelieferte LiberationSans-Regular. Eine fette Variante
     * davon liefert OpenPDF nicht mit, der Fettdruck aus Font.BOLD faellt also ersatzlos weg.
     * Mit explizit gesetzter BaseFont greift die Ersetzung nicht, und Helvetica-Bold kennt in
     * WinAnsi-Kodierung das Euro-Zeichen. Der Style bleibt bewusst ungesetzt: zu einer expliziten
     * BaseFont wuerde Font.BOLD zusaetzlich kuenstlichen Fettdruck ueber den Zeichenrand legen. */
    private Font betragFett(float groesse) throws DocumentException, IOException {
        return new Font(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI,
                BaseFont.NOT_EMBEDDED), groesse);
    }

    /** Briefkopf-Zeilen aus den Vereins-Stammdaten (siehe VereinDialogController) - leer, wenn
     * noch keine Vereinsdaten hinterlegt sind (Berichte sollen auch ohne das funktionieren). */
    private List<Paragraph> vereinKopf(Font nameFont, Font detailFont) {
        List<Paragraph> zeilen = new ArrayList<>();
        Optional<Verein> verein = vereinRepository.find();
        if (verein.isEmpty()) {
            return zeilen;
        }
        Verein v = verein.get();
        zeilen.add(new Paragraph(v.getName(), nameFont));
        String adresse = vereinAdresse(v);
        if (!adresse.isBlank()) {
            zeilen.add(new Paragraph(adresse, detailFont));
        }
        String kontakt = vereinKontakt(v);
        if (!kontakt.isBlank()) {
            zeilen.add(new Paragraph(kontakt, detailFont));
        }
        String bankverbindung = vereinBankverbindung(v);
        if (!bankverbindung.isBlank()) {
            zeilen.add(new Paragraph(bankverbindung, detailFont));
        }
        zeilen.add(leerzeile());
        return zeilen;
    }

    private String vereinAdresse(Verein v) {
        String strasse = v.getStrasse() != null ? v.getStrasse() : "";
        String ortsteil = ((v.getPlz() != null ? v.getPlz() : "") + " " + (v.getOrt() != null ? v.getOrt() : "")).trim();
        return (strasse + (!strasse.isBlank() && !ortsteil.isBlank() ? ", " : "") + ortsteil).trim();
    }

    private String vereinKontakt(Verein v) {
        List<String> teile = new ArrayList<>();
        if (v.getTelefon() != null && !v.getTelefon().isBlank()) {
            teile.add("Tel: " + v.getTelefon());
        }
        if (v.getEmail() != null && !v.getEmail().isBlank()) {
            teile.add("E-Mail: " + v.getEmail());
        }
        if (v.getHomepage() != null && !v.getHomepage().isBlank()) {
            teile.add("Web: " + v.getHomepage());
        }
        return String.join("  ·  ", teile);
    }

    private String vereinBankverbindung(Verein v) {
        if (v.getIban() == null || v.getIban().isBlank()) {
            return "";
        }
        String bic = v.getBic() != null && !v.getBic().isBlank() ? " / " + v.getBic() : "";
        String bank = v.getBankname() != null && !v.getBankname().isBlank() ? " (" + v.getBankname() + ")" : "";
        return "Bankverbindung: " + v.getIban() + bic + bank;
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
        return kopfzelle(text, new Font(Font.HELVETICA, 10, Font.BOLD));
    }

    private PdfPCell kopfzelle(String text, Font font) {
        PdfPCell cell = new PdfPCell(new org.openpdf.text.Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private PdfPCell betragZelle(long cent) {
        PdfPCell cell = new PdfPCell(new org.openpdf.text.Phrase(BETRAG_FORMAT.format(BigDecimal.valueOf(cent, 2))));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private PdfPCell betragZelle(long cent, Font font) {
        PdfPCell cell = new PdfPCell(
                new org.openpdf.text.Phrase(BETRAG_FORMAT.format(BigDecimal.valueOf(cent, 2)), font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    private static final String[] MITGLIEDER_SPALTEN = {
            "Id", "Anrede", "Vorname", "Nachname", "Straße", "PLZ", "Ort", "Telefon", "E-Mail", "Eintrittsdatum",
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
                zeile.createCell(7).setCellValue(wert(m.getTelefon()));
                zeile.createCell(8).setCellValue(wert(m.getEmail()));
                zeile.createCell(9).setCellValue(wert(m.getEintrittsdatum()));
                zeile.createCell(10).setCellValue(wert(m.getAustrittsdatum()));
                zeile.createCell(11).setCellValue(m.getStatus().name());
                zeile.createCell(12).setCellValue(m.getJahresbeitragEuro() != null
                        ? m.getJahresbeitragEuro().toPlainString().replace('.', ',') : "");
                zeile.createCell(13).setCellValue(wert(m.getIban()));
                zeile.createCell(14).setCellValue(wert(m.getBic()));
                zeile.createCell(15).setCellValue(m.getMandatsreferenz() != null
                        ? String.valueOf(m.getMandatsreferenz()) : "");
                zeile.createCell(16).setCellValue(wert(m.getSammellastschrift()));
                zeile.createCell(17).setCellValue(wert(m.getNotiz()));
            }

            for (int i = 0; i < MITGLIEDER_SPALTEN.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(ziel.toFile())) {
                workbook.write(out);
            }
        }
    }

    /** Beruecksichtigt eine ausdrueckliche Geschaeftsjahr-Zuordnung der Buchung, die vom
     * Buchungsdatum abweichen kann (siehe GeschaeftsjahrService.findeFuer) - sonst wuerde der
     * Kassenbericht am Stichtag andere Buchungen zeigen als die Saldo-Spalte der Uebersicht. */
    /** Die Buchungen, die Kassenbericht und Kassenpruefbericht zugrunde legen - gemeinsame
     * Stelle, damit beide Dokumente nie unterschiedliche Mengen auswerten. */
    private List<Buchung> buchungenFuer(Konto konto, Geschaeftsjahr jahr) {
        return buchungRepository.findAll().stream()
                .filter(b -> b.getKontoId().equals(konto.getId()))
                .filter(b -> !b.isStorniert())
                .filter(BerichtService::hatBetrag)
                .filter(b -> gehoertZu(b, jahr))
                // Nach Datum UND id: findAll() liefert absteigend, ohne die id waere die
                // Reihenfolge mehrerer Buchungen desselben Tages verdreht - der laufende Saldo
                // des Journals liefe dann nicht monoton (siehe berechneLaufendeSalden, das
                // genauso sortiert).
                .sorted(Comparator.comparing(Buchung::getBuchungsdatum).thenComparing(Buchung::getId))
                .toList();
    }

    /** Buchungen ohne Betrag bleiben in beiden Berichten unberuecksichtigt: sie sind weder
     * Einnahme noch Ausgabe. Sie entstehen beim HBCI-Import aus reinen Mitteilungen der Bank
     * ("Abrechnung ... siehe Anlage"), der anders als die manuelle Erfassung keine
     * Betragspruefung durchlaeuft. Ohne diesen Filter wuerden sie wegen der Einordnung
     * betrag >= 0 als Einnahme gezaehlt und dort eine Zeile mit 0,00 Euro erzeugen - meist
     * unter "(ohne Kategorie)", da solche Mitteilungen keine Kategorie tragen. In der
     * Buchungsuebersicht bleiben sie sichtbar, damit sie zum Kontoauszug passt. */
    private static boolean hatBetrag(Buchung buchung) {
        return buchung.getBetragCent() != 0;
    }

    private boolean gehoertZu(Buchung buchung, Geschaeftsjahr jahr) {
        if (buchung.getGeschaeftsjahrId() != null) {
            return buchung.getGeschaeftsjahrId().equals(jahr.getId());
        }
        return jahr.enthaelt(buchung.getBuchungsdatum());
    }

    private String wert(String text) {
        return text != null ? text : "";
    }

    /** Liest eine Excel-Datei (.xlsx) im exakt gleichen Format wie exportiereMitgliederliste
     * (gleiche Spaltenreihenfolge, gleiche Kopfzeile) und legt je Datenzeile ein neues Mitglied
     * an - ueber MitgliedService.save(...), damit dieselbe Validierung/Normalisierung greift wie
     * bei manueller Erfassung. Fehlerhafte Zeilen (z. B. fehlender Nachname) werden uebersprungen
     * und gesammelt zurueckgegeben, statt den gesamten Import abzubrechen. */
    /** Importiert die Mitgliederliste ganz oder gar nicht: zuerst wird die komplette Datei
     * geprueft, und nur wenn keine einzige Zeile beanstandet wird, werden alle Mitglieder
     * angelegt. Bei Fehlern bleibt die Datenbank unberuehrt, damit ein korrigierter zweiter
     * Versuch nicht auf einem halb importierten Bestand aufsetzt (und Zeilen doppelt anlegt).
     * Die Zeilennummern in den Meldungen sind die der Tabelle, also 1-basiert inkl. Kopfzeile. */
    public MitgliederImportErgebnis importiereMitgliederliste(Path quelle) throws IOException {
        List<Mitglied> zuImportieren = new ArrayList<>();
        List<String> fehler = new ArrayList<>();

        try (FileInputStream in = new FileInputStream(quelle.toFile());
             XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            pruefeKopfzeile(sheet);

            DataFormatter formatter = new DataFormatter();
            for (int zeilenIndex = 1; zeilenIndex <= sheet.getLastRowNum(); zeilenIndex++) {
                Row zeile = sheet.getRow(zeilenIndex);
                if (zeile == null
                        || (zellenwert(formatter, zeile, 2).isBlank() && zellenwert(formatter, zeile, 3).isBlank())) {
                    continue;
                }
                List<String> fehlerDerZeile = new ArrayList<>();
                Mitglied mitglied = mitgliedAusZeile(formatter, zeile, fehlerDerZeile);
                if (fehlerDerZeile.isEmpty()) {
                    zuImportieren.add(mitglied);
                } else {
                    int zeilenNummer = zeilenIndex + 1;
                    fehlerDerZeile.forEach(f -> fehler.add("Zeile " + zeilenNummer + ": " + f));
                }
            }
        }

        if (zuImportieren.isEmpty() && fehler.isEmpty()) {
            fehler.add("Die Datei enthält keine Datenzeilen.");
        }
        if (!fehler.isEmpty()) {
            // Bewusst vor jedem Schreibzugriff: nichts wurde angelegt.
            return new MitgliederImportErgebnis(0, fehler);
        }
        mitgliedService.importiere(zuImportieren);
        return new MitgliederImportErgebnis(zuImportieren.size(), List.of());
    }

    private void pruefeKopfzeile(Sheet sheet) throws IOException {
        Row kopfzeile = sheet.getRow(0);
        DataFormatter formatter = new DataFormatter();
        if (kopfzeile == null || !MITGLIEDER_SPALTEN[0].equals(zellenwert(formatter, kopfzeile, 0))) {
            throw new IOException("Datei entspricht nicht dem erwarteten Format der Mitgliederliste "
                    + "(Kopfzeile fehlt oder passt nicht) - bitte eine zuvor exportierte Mitgliederliste verwenden.");
        }
    }

    /** Liest eine Zeile und sammelt alle Beanstandungen in fehlerDerZeile, statt beim ersten
     * Problem abzubrechen - sonst verdeckt der erste Fehler die weiteren derselben Zeile und der
     * Nutzer muesste die Datei mehrfach korrigieren und erneut importieren. Das zurueckgegebene
     * Mitglied ist nur verwendbar, wenn die Liste leer geblieben ist. */
    private Mitglied mitgliedAusZeile(DataFormatter formatter, Row zeile, List<String> fehlerDerZeile) {
        // Spalte 0 (Id) wird bewusst nicht gelesen - jede importierte Zeile legt ein neues
        // Mitglied mit neu vergebener Id an, siehe Klassenkommentar von importiereMitgliederliste.
        Mitglied mitglied = new Mitglied();
        String anredeText = zellenwert(formatter, zeile, 1);
        try {
            mitglied.setAnrede(anredeText.isBlank() ? null : Anrede.valueOf(anredeText.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            fehlerDerZeile.add("Anrede \"" + anredeText + "\" ist unbekannt (erlaubt: Herr, Frau, Familie)");
        }
        mitglied.setVorname(zellenwert(formatter, zeile, 2));
        mitglied.setNachname(zellenwert(formatter, zeile, 3));
        mitglied.setStrasse(leerZuNull(zellenwert(formatter, zeile, 4)));
        mitglied.setPlz(leerZuNull(zellenwert(formatter, zeile, 5)));
        mitglied.setOrt(leerZuNull(zellenwert(formatter, zeile, 6)));
        mitglied.setTelefon(leerZuNull(zellenwert(formatter, zeile, 7)));
        mitglied.setEmail(leerZuNull(zellenwert(formatter, zeile, 8)));
        mitglied.setEintrittsdatum(leerZuNull(zellenwert(formatter, zeile, 9)));
        mitglied.setAustrittsdatum(leerZuNull(zellenwert(formatter, zeile, 10)));
        String statusText = zellenwert(formatter, zeile, 11);
        try {
            mitglied.setStatus(statusText.isBlank()
                    ? MitgliedStatus.AKTIV : MitgliedStatus.valueOf(statusText.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            fehlerDerZeile.add("Status \"" + statusText + "\" ist unbekannt (erlaubt: Aktiv, Inaktiv)");
        }
        String jahresbeitragText = zellenwert(formatter, zeile, 12);
        try {
            mitglied.setJahresbeitragEuro(jahresbeitragText.isBlank()
                    ? null : new BigDecimal(jahresbeitragText.replace(',', '.')));
        } catch (NumberFormatException e) {
            fehlerDerZeile.add("Jahresbeitrag \"" + jahresbeitragText + "\" ist kein Betrag");
        }
        mitglied.setIban(leerZuNull(zellenwert(formatter, zeile, 13)));
        mitglied.setBic(leerZuNull(zellenwert(formatter, zeile, 14)));
        String mandatsreferenzText = zellenwert(formatter, zeile, 15);
        try {
            mitglied.setMandatsreferenz(mandatsreferenzText.isBlank() ? null : Long.parseLong(mandatsreferenzText));
        } catch (NumberFormatException e) {
            fehlerDerZeile.add("Mandatsreferenz \"" + mandatsreferenzText + "\" ist keine Ganzzahl");
        }
        mitglied.setSammellastschrift(leerZuNull(zellenwert(formatter, zeile, 16)));
        mitglied.setNotiz(leerZuNull(zellenwert(formatter, zeile, 17)));

        // Dieselben Regeln, die auch beim Speichern gelten - sonst koennte die Datei als
        // fehlerfrei durchgehen und das Einfuegen trotzdem scheitern.
        try {
            mitgliedService.pruefeUndNormalisiere(mitglied);
        } catch (IllegalArgumentException e) {
            fehlerDerZeile.add(e.getMessage());
        }
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
