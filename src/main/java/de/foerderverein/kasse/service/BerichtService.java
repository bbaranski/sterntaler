package de.foerderverein.kasse.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
 * Mitgliederliste als CSV. Bewusst schlicht gehalten (Tabellen, keine grafische Auswertung) -
 * fuer den vorgesehenen Zweck (Ausdruck/Ablage, Vorlage beim Kassenpruefer) ausreichend. */
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

    /** Einfache CSV-Datei (Semikolon-getrennt, da Excel im DACH-Raum das Komma als
     * Dezimaltrennzeichen erwartet und sonst falsch importiert). */
    public void exportiereMitgliederliste(Path ziel) throws IOException {
        List<Mitglied> mitglieder = new ArrayList<>(mitgliedRepository.findAll());
        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(ziel.toFile()), StandardCharsets.UTF_8)) {
            writer.write("Vorname;Nachname;Strasse;PLZ;Ort;E-Mail;Eintrittsdatum;Austrittsdatum;Status\n");
            for (Mitglied m : mitglieder) {
                writer.write(csv(m.getVorname()) + ";" + csv(m.getNachname()) + ";" + csv(m.getStrasse()) + ";"
                        + csv(m.getPlz()) + ";" + csv(m.getOrt()) + ";" + csv(m.getEmail()) + ";"
                        + csv(m.getEintrittsdatum() != null ? m.getEintrittsdatum().format(DATUM_FORMAT) : null) + ";"
                        + csv(m.getAustrittsdatum() != null ? m.getAustrittsdatum().format(DATUM_FORMAT) : null) + ";"
                        + csv(m.getStatus().name()) + "\n");
            }
        }
    }

    private String csv(String wert) {
        if (wert == null) {
            return "";
        }
        String bereinigt = wert.replace("\"", "\"\"");
        return bereinigt.contains(";") || bereinigt.contains("\"") ? "\"" + bereinigt + "\"" : bereinigt;
    }
}
