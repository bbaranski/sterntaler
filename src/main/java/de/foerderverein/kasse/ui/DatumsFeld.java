package de.foerderverein.kasse.ui;

import java.time.LocalDate;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import de.foerderverein.kasse.domain.Datumstext;

/** Deutsches Datumsformat fuer DatePicker-Felder: TT.MM.JJJJ, angezeigt und eingetippt.
 *
 * <p>Ohne diese Konfiguration nimmt JavaFX das kurze Format der Systemsprache - in einer
 * deutschen Umgebung also "01.03.26" mit zweistelliger Jahreszahl, was bei Jahresangaben einer
 * Kasse zu Verwechslungen einlaedt.
 *
 * <p>Welche Schreibweisen gelesen und geschrieben werden, steht in Datumstext - dieselbe
 * Umwandlung benutzen die Mitgliedertabelle und der Mitgliederexport. */
final class DatumsFeld {

    private DatumsFeld() {
    }

    /** Setzt Format und Platzhalter. Danach zeigt das Feld TT.MM.JJJJ an - sowohl bei Auswahl im
     * Kalender als auch nach dem Eintippen. */
    static void konfiguriere(DatePicker feld) {
        feld.setPromptText("TT.MM.JJJJ");
        feld.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate datum) {
                return Datumstext.text(datum);
            }

            @Override
            public LocalDate fromString(String eingabe) {
                return Datumstext.parse(eingabe);
            }
        });
    }
}
