package de.foerderverein.kasse.doc;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

/** Erzeugt docs/handbuch.pdf aus docs/handbuch.md und schreibt daneben die Pruefsumme der
 * Quelldatei. Aufruf: {@code mvn -o compile exec:java@handbuch}.
 *
 * <p>Die Pruefsumme ist der Grund, warum das PDF nicht veralten kann: HandbuchPdfTest rechnet sie
 * nach und schlaegt fehl, sobald das Handbuch geaendert, das PDF aber nicht neu erzeugt wurde.
 * Das Format entspricht dem von {@code shasum}, die Datei laesst sich also auch mit
 * {@code shasum -c docs/handbuch.md.sha256} pruefen.
 *
 * <p>Laeuft nur mit den "provided"-Abhaengigkeiten flexmark und openhtmltopdf (siehe pom.xml) und
 * gehoert damit nicht zum Funktionsumfang der ausgelieferten Anwendung - die zeigt das fertige
 * PDF lediglich an (MainController.onHandbuch). */
public final class HandbuchPdf {

    private static final Path QUELLE = Path.of("docs", "handbuch.md");
    private static final Path ZIEL = Path.of("docs", "handbuch.pdf");
    private static final Path PRUEFSUMME = Path.of("docs", "handbuch.md.sha256");
    private static final String ERSATZSCHRIFT = "/font-fallback/LiberationSans-Regular.ttf";
    /** Menuepfade im Handbuch ("Einstellungen → Kategorien"); kennt keine eingebaute PDF-Schrift. */
    private static final String PFEIL = "\u2192";

    private HandbuchPdf() {
    }

    public static void main(String[] args) throws Exception {
        String markdown = Files.readString(QUELLE, StandardCharsets.UTF_8);

        // RENDER_HEADER_ID: GitHub vergibt die Sprungmarken der Ueberschriften von selbst, im
        // PDF muessen sie im HTML stehen - sonst zeigt das Inhaltsverzeichnis ins Leere.
        MutableDataSet optionen = new MutableDataSet()
                .set(Parser.EXTENSIONS, List.of(TablesExtension.create()))
                .set(HtmlRenderer.GENERATE_HEADER_ID, true)
                .set(HtmlRenderer.RENDER_HEADER_ID, true);
        Node dokument = Parser.builder(optionen).build().parse(markdown);
        String rumpf = HtmlRenderer.builder(optionen).build().render(dokument);
        // Nur das Pfeilzeichen bekommt die Ersatzschrift - stuende sie in der Schriftfamilie des
        // ganzen Dokuments, waehlte openhtmltopdf sie auch fuer fetten Text und wuerde ihn mangels
        // fetter Variante kuenstlich verbreitern, mitten im sonst einheitlichen Helvetica-Satz.
        rumpf = rumpf.replace(PFEIL, "<span class=\"pfeil\">" + PFEIL + "</span>");

        try (OutputStream aus = Files.newOutputStream(ZIEL)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // Die eingebauten PDF-Schriften kennen das Pfeilzeichen nicht und setzen stattdessen
            // ein "#". Als Ersatzschrift dient die LiberationSans, die in openpdf ohnehin
            // mitgeliefert wird - so braucht das Projekt keine eigene Schriftdatei.
            builder.useFont(() -> HandbuchPdf.class.getResourceAsStream(ERSATZSCHRIFT),
                    "LiberationSans");
            builder.withHtmlContent(seite(rumpf), null);
            builder.toStream(aus);
            builder.run();
        }

        Files.writeString(PRUEFSUMME, sha256(markdown) + "  " + QUELLE.getFileName() + "\n",
                StandardCharsets.UTF_8);

        System.out.println(ZIEL + " erzeugt (" + Files.size(ZIEL) / 1024 + " kB)");
    }

    /** Berechnet die Pruefsumme ueber den Text, nicht ueber die Bytes der Datei - so haengt sie
     * nicht an der Zeilenende-Konvention, mit der jemand die Datei ausgecheckt hat. */
    static String sha256(String text) throws Exception {
        byte[] summe = MessageDigest.getInstance("SHA-256")
                .digest(text.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(summe.length * 2);
        for (byte b : summe) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /** Das HTML-Geruest um den gerenderten Markdown-Rumpf. openhtmltopdf liest XHTML, deshalb
     * steht hier eine XML-Deklaration; die Seitenzahl in der Fusszeile kommt aus CSS Paged Media
     * und muss nicht im Markdown stehen. Bewusst zusammengesetzt statt ueber formatted(...): das
     * CSS enthaelt Prozentwerte, die ein Formatierer als Platzhalter missverstehen wuerde. */
    private static String seite(String rumpf) {
        return KOPF + rumpf + FUSS;
    }

    private static final String KOPF = """
                <?xml version="1.0" encoding="UTF-8"?>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                  <title>Nutzerhandbuch Sterntaler</title>
                  <style>
                    @page {
                      size: A4;
                      margin: 22mm 20mm 20mm 20mm;
                      @bottom-center {
                        content: "Seite " counter(page) " von " counter(pages);
                        font-family: Helvetica, sans-serif;
                        font-size: 9pt;
                        color: #555555;
                      }
                    }
                    body { font-family: Helvetica, sans-serif; font-size: 10.5pt; line-height: 1.45; color: #111111; }
                    h1 { font-size: 22pt; margin: 0 0 4pt 0; }
                    h2 { font-size: 14pt; margin: 22pt 0 6pt 0; page-break-after: avoid; }
                    h2, h3 { page-break-inside: avoid; }
                    p, ul, ol { margin: 0 0 8pt 0; }
                    li { margin-bottom: 3pt; }
                    code { font-family: Courier, monospace; font-size: 9.5pt; background-color: #f2f2f2; }
                    a { color: #111111; text-decoration: none; }
                    .pfeil { font-family: "LiberationSans", sans-serif; }
                    table { border-collapse: collapse; width: 100%; margin: 0 0 10pt 0; page-break-inside: avoid; }
                    th, td { border: 0.5pt solid #999999; padding: 3pt 5pt; text-align: left; vertical-align: top; }
                    th { background-color: #f2f2f2; }
                  </style>
                </head>
                <body>
                """;

    private static final String FUSS = """

                </body>
                </html>
                """;
}
