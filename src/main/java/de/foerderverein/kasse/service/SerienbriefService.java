package de.foerderverein.kasse.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;

import de.foerderverein.kasse.domain.Briefvorlage;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.Verein;
import de.foerderverein.kasse.persistence.VereinRepository;

/** Erzeugt Serienbriefe aus einer vom Nutzer selbst in Word gestalteten .docx-Vorlage mit
 * Platzhaltern (siehe BEKANNTE_PLATZHALTER) - entweder als zusammengefuehrte Druck-Datei (ein
 * Mitglied pro Seite) oder als Text fuer eine einzelne Sammel-E-Mail (alle Empfaenger im BCC).
 *
 * <p>Wichtig fuer Vorlagenersteller: Platzhalter wie {{Vorname}} in der Kopf-/Fusszeile werden
 * nur EINMAL ersetzt (mit den Werten des ersten Mitglieds), da eine Word-Kopf-/Fusszeile pro
 * zusammengefuehrtem Dokument nur einmal existiert und sich nicht pro "eingefuegtem" Mitglied
 * wiederholen laesst. Mitglieder-bezogene Platzhalter gehoeren daher in den Dokumentkoerper,
 * nicht in Kopf-/Fusszeile - Vereins-Platzhalter (z. B. {{VereinsName}}) sind dort unproblematisch,
 * da sie fuer alle Mitglieder ohnehin gleich sind. */
public class SerienbriefService {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Pattern PLATZHALTER_MUSTER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*\\}\\}");
    private static final String ALLGEMEINE_ANREDE = "Liebe Mitglieder,";

    public static final List<String> BEKANNTE_PLATZHALTER = List.of(
            "Vorname", "Nachname", "Strasse", "Plz", "Ort", "Datum", "Brieftext",
            "SignaturName", "SignaturRolle", "VereinsName", "VereinsStrasse", "VereinsPlz",
            "VereinsOrt", "VereinsTelefon", "VereinsEmail", "Bankverbindung");

    /** Ergebnis von erstelleMailEntwurf: ein einzelner Entwurf fuer eine Sammel-E-Mail mit allen
     * erreichbaren Mitgliedern im BCC; ohneEmail listet die dabei ausgelassenen Mitglieder. */
    public record SerienbriefMailEntwurf(List<String> bccAdressen, String betreff, String text,
                                          List<Mitglied> ohneEmail) {
    }

    private final Path vorlagenVerzeichnis;
    private final VereinRepository vereinRepository;

    public SerienbriefService(Path vorlagenVerzeichnis, VereinRepository vereinRepository) {
        this.vorlagenVerzeichnis = vorlagenVerzeichnis;
        this.vereinRepository = vereinRepository;
    }

    /** Non-blocking-Pruefung fuer die Vorlagenverwaltung: enthaelt die Datei ueberhaupt einen der
     * bekannten Platzhalter? Dient nur als Hinweis, kein Fehler. */
    public boolean enthaeltBekanntenPlatzhalter(File docxDatei) throws IOException {
        try (var in = Files.newInputStream(docxDatei.toPath());
             XWPFDocument doc = new XWPFDocument(in)) {
            String gesamtText = gesamtText(doc);
            for (String platzhalter : BEKANNTE_PLATZHALTER) {
                if (gesamtText.contains("{{" + platzhalter + "}}")) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Erzeugt eine zusammengefuehrte .docx-Datei mit einer Seite je Mitglied: fuer jedes
     * Mitglied wird eine frische Kopie der Vorlagen-Bytes geladen und befuellt; die erste Kopie
     * dient als Basisdokument (uebernimmt Kopf-/Fusszeilen, Stile), die Koerperinhalte der
     * uebrigen Kopien werden - durch einen Seitenumbruch getrennt - in den Koerper des
     * Basisdokuments uebernommen. Zulaessig, da alle Kopien aus derselben Vorlage stammen und
     * daher identische Stile/Kopf-/Fusszeilen haben. */
    public Path erstelleSammeldruckDocx(Briefvorlage vorlage, List<Mitglied> empfaenger, String brieftext,
                                         String signaturName, String signaturRolle, Path ziel) throws IOException {
        if (empfaenger.isEmpty()) {
            throw new IllegalArgumentException("Es wurden keine Empfaenger ausgewaehlt.");
        }
        byte[] vorlageBytes = Files.readAllBytes(vorlagenVerzeichnis.resolve(vorlage.getSpeicherpfad()));
        Verein verein = vereinRepository.find().orElse(null);

        try (XWPFDocument basis = new XWPFDocument(new ByteArrayInputStream(vorlageBytes))) {
            ersetzePlatzhalter(basis, werteFuerMitglied(empfaenger.get(0), brieftext, signaturName, signaturRolle, verein));
            CTBody basisBody = basis.getDocument().getBody();

            for (int i = 1; i < empfaenger.size(); i++) {
                try (XWPFDocument kopie = new XWPFDocument(new ByteArrayInputStream(vorlageBytes))) {
                    Map<String, String> werte = werteFuerMitglied(empfaenger.get(i), brieftext, signaturName,
                            signaturRolle, verein);
                    ersetzePlatzhalter(kopie, werte);

                    XWPFParagraph umbruch = basis.createParagraph();
                    umbruch.createRun().addBreak(BreakType.PAGE);

                    for (IBodyElement element : kopie.getBodyElements()) {
                        if (element instanceof XWPFParagraph p) {
                            basisBody.addNewP().set(p.getCTP());
                        } else if (element instanceof XWPFTable t) {
                            basisBody.addNewTbl().set(t.getCTTbl());
                        }
                    }
                }
            }

            Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
            try (var out = Files.newOutputStream(ziel)) {
                basis.write(out);
            }
        }
        return ziel;
    }

    /** Liefert einen einzelnen Mail-Entwurf fuer eine Sammel-E-Mail (alle erreichbaren Mitglieder
     * im BCC) - reiner Text, unabhaengig von der Word-Vorlage. */
    public SerienbriefMailEntwurf erstelleMailEntwurf(List<Mitglied> empfaenger, String betreff, String brieftext,
                                                       String signaturName, String signaturRolle) {
        List<String> bcc = new ArrayList<>();
        List<Mitglied> ohneEmail = new ArrayList<>();
        for (Mitglied mitglied : empfaenger) {
            if (mitglied.getEmail() != null && !mitglied.getEmail().isBlank()) {
                bcc.add(mitglied.getEmail());
            } else {
                ohneEmail.add(mitglied);
            }
        }
        String text = briefinhalt(brieftext, signaturName, signaturRolle);
        return new SerienbriefMailEntwurf(bcc, betreff, text, ohneEmail);
    }

    private String briefinhalt(String brieftext, String signaturName, String signaturRolle) {
        StringBuilder text = new StringBuilder();
        text.append(ALLGEMEINE_ANREDE).append("\n\n");
        text.append(wert(brieftext)).append("\n\n");
        text.append("Mit freundlichen Grüßen");
        if (signaturName != null && !signaturName.isBlank()) {
            text.append("\n").append(signaturName);
        }
        if (signaturRolle != null && !signaturRolle.isBlank()) {
            text.append("\n").append(signaturRolle);
        }
        return text.toString();
    }

    private Map<String, String> werteFuerMitglied(Mitglied mitglied, String brieftext, String signaturName,
                                                    String signaturRolle, Verein verein) {
        Map<String, String> werte = new LinkedHashMap<>();
        werte.put("Vorname", wert(mitglied.getVorname()));
        werte.put("Nachname", wert(mitglied.getNachname()));
        werte.put("Strasse", wert(mitglied.getStrasse()));
        werte.put("Plz", wert(mitglied.getPlz()));
        werte.put("Ort", wert(mitglied.getOrt()));
        werte.put("Datum", LocalDate.now().format(DATUM_FORMAT));
        werte.put("Brieftext", wert(brieftext));
        werte.put("SignaturName", wert(signaturName));
        werte.put("SignaturRolle", wert(signaturRolle));
        if (verein != null) {
            werte.put("VereinsName", wert(verein.getName()));
            werte.put("VereinsStrasse", wert(verein.getStrasse()));
            werte.put("VereinsPlz", wert(verein.getPlz()));
            werte.put("VereinsOrt", wert(verein.getOrt()));
            werte.put("VereinsTelefon", wert(verein.getTelefon()));
            werte.put("VereinsEmail", wert(verein.getEmail()));
            werte.put("Bankverbindung", bankverbindung(verein));
        }
        return werte;
    }

    private String bankverbindung(Verein verein) {
        if (verein.getIban() == null || verein.getIban().isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("IBAN: ").append(verein.getIban());
        if (verein.getBic() != null && !verein.getBic().isBlank()) {
            sb.append(", BIC: ").append(verein.getBic());
        }
        if (verein.getBankname() != null && !verein.getBankname().isBlank()) {
            sb.append(", ").append(verein.getBankname());
        }
        return sb.toString();
    }

    private void ersetzePlatzhalter(XWPFDocument doc, Map<String, String> werte) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            ersetzeInAbsatz(p, werte);
        }
        for (XWPFTable t : doc.getTables()) {
            ersetzeInTabelle(t, werte);
        }
        for (XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph p : header.getParagraphs()) {
                ersetzeInAbsatz(p, werte);
            }
            for (XWPFTable t : header.getTables()) {
                ersetzeInTabelle(t, werte);
            }
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            for (XWPFParagraph p : footer.getParagraphs()) {
                ersetzeInAbsatz(p, werte);
            }
            for (XWPFTable t : footer.getTables()) {
                ersetzeInTabelle(t, werte);
            }
        }
    }

    private void ersetzeInTabelle(XWPFTable table, Map<String, String> werte) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                    ersetzeInAbsatz(p, werte);
                }
            }
        }
    }

    /** Word zerlegt sichtbaren Text intern oft in mehrere "Runs" (z. B. durch
     * Rechtschreibpruefung oder Formatierungswechsel) - ein {{Platzhalter}} kann dadurch ueber
     * mehrere Runs verteilt sein. Robuste Ersetzung daher je Absatz: Text aller Runs
     * zusammenfuegen, Platzhalter ersetzen, Ergebnis in den ersten Run zurueckschreiben,
     * ueberzaehlige Runs leeren - Standardtechnik fuer POI-basiertes Mail-Merge. */
    private void ersetzeInAbsatz(XWPFParagraph absatz, Map<String, String> werte) {
        List<XWPFRun> runs = absatz.getRuns();
        if (runs.isEmpty()) {
            return;
        }
        StringBuilder text = new StringBuilder();
        for (XWPFRun run : runs) {
            String t = run.getText(0);
            text.append(t != null ? t : "");
        }
        String original = text.toString();
        String ersetzt = ersetzePlatzhalterInText(original, werte);
        if (ersetzt.equals(original)) {
            return;
        }
        schreibeMehrzeilig(runs.get(0), ersetzt);
        for (int i = 1; i < runs.size(); i++) {
            runs.get(i).setText("", 0);
        }
    }

    /** Schreibt den Text in den Run und wandelt dabei Zeilenumbrueche in echte Word-Umbrueche
     * (w:br) um - ein blosses "\n" in einem w:t-Element stellt Word sonst nicht als Zeilenumbruch
     * dar, sondern laesst es weg. Wichtig fuer mehrzeilige Brieftexte (z. B. Tagesordnungen). */
    private void schreibeMehrzeilig(XWPFRun run, String text) {
        String[] zeilen = text.split("\n", -1);
        run.setText(zeilen[0], 0);
        for (int i = 1; i < zeilen.length; i++) {
            run.addBreak();
            run.setText(zeilen[i], i);
        }
    }

    private String ersetzePlatzhalterInText(String text, Map<String, String> werte) {
        Matcher matcher = PLATZHALTER_MUSTER.matcher(text);
        StringBuilder ergebnis = new StringBuilder();
        while (matcher.find()) {
            String ersatz = werte.get(matcher.group(1));
            matcher.appendReplacement(ergebnis, Matcher.quoteReplacement(ersatz != null ? ersatz : matcher.group()));
        }
        matcher.appendTail(ergebnis);
        return ergebnis.toString();
    }

    private String gesamtText(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph p : doc.getParagraphs()) {
            sb.append(p.getText());
        }
        for (XWPFTable t : doc.getTables()) {
            sammleTabellenText(t, sb);
        }
        for (XWPFHeader header : doc.getHeaderList()) {
            for (XWPFParagraph p : header.getParagraphs()) {
                sb.append(p.getText());
            }
        }
        for (XWPFFooter footer : doc.getFooterList()) {
            for (XWPFParagraph p : footer.getParagraphs()) {
                sb.append(p.getText());
            }
        }
        return sb.toString();
    }

    private void sammleTabellenText(XWPFTable table, StringBuilder sb) {
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                    sb.append(p.getText());
                }
            }
        }
    }

    private static String wert(String text) {
        return text != null ? text : "";
    }
}
