package de.foerderverein.kasse.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/** Die eine Schreibweise fuer Datumsangaben, die als Text gespeichert sind: Eintritts- und
 * Austrittsdatum eines Mitglieds (Spalten vom Typ TEXT, siehe V1__init.sql).
 *
 * <p>Geschrieben wird immer TT.MM.JJJJ - in der Oberflaeche, in der Mitgliedertabelle und im
 * Mitgliederexport. Gelesen wird grosszuegiger, weil die Felder frueher freie Textfelder waren
 * und der Mitgliederimport den Zellentext uebernimmt, so wie Excel ihn anzeigt: Eine Datei im
 * US-Format liefert dabei "9/26/26".
 *
 * <p>Bewusst hier im Domaenenpaket und nicht in der Oberflaeche: Dieselbe Umwandlung brauchen
 * Tabelle, Dialog und Export, und der Export liegt im Dienstpaket. */
public final class Datumstext {

    /** Die verbindliche Schreibweise. */
    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Zusaetzlich beim Lesen akzeptiert. "dd.MM.yyyy" verlangt zwei Stellen je Feld und
     * scheitert an "1.3.2026".
     *
     * <p>Bei den Formaten mit Schraegstrich gilt Monat vor Tag - sie stammen aus Importen von
     * US-formatierten Dateien, und mit Punkten geschriebene Daten fangen die Formate darueber
     * ab. */
    private static final List<DateTimeFormatter> LESEFORMATE = List.of(
            FORMAT,
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"));

    /** Chronologische Reihenfolge fuer gespeicherte Datumstexte - fuer die Datumsspalten der
     * Mitgliedertabelle, die sonst ihren Text alphabetisch sortieren wuerden: dort stuende der
     * 01.03.2026 vor dem 15.02.2025.
     *
     * <p>Was sich nicht als Datum lesen laesst, auch Leeres, sortiert hinten - und dort nach
     * seinem Text, damit die Reihenfolge nachvollziehbar bleibt und nicht von der
     * Ausgangssortierung abhaengt. */
    public static final Comparator<String> VERGLEICH =
            Comparator.<String, LocalDate>comparing(Datumstext::parse,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(text -> text == null ? "" : text);

    private Datumstext() {
    }

    /** null, wenn der Text leer ist oder in keinem der Leseformate steht. */
    public static LocalDate parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String eingabe = text.trim();
        for (DateTimeFormatter format : LESEFORMATE) {
            try {
                return LocalDate.parse(eingabe, format);
            } catch (DateTimeParseException naechstesFormat) {
                // absichtlich leer - das naechste Format ist dran
            }
        }
        return null;
    }

    /** Leerer Text fuer null, damit ein leeres Feld leer bleibt und nicht "null" anzeigt. */
    public static String text(LocalDate datum) {
        return datum != null ? datum.format(FORMAT) : "";
    }

    /** Ein gespeicherter Datumstext, wie er angezeigt werden soll: in TT.MM.JJJJ umgeschrieben,
     * sofern er sich lesen laesst. Was nicht zu lesen ist, bleibt unveraendert stehen - lieber
     * eine krumme Schreibweise zeigen als eine leere Zelle. Die gespeicherten Werte selbst ruehrt
     * das nicht an; die werden erst beim naechsten Speichern des Mitglieds vereinheitlicht
     * (siehe MitgliedDialogController). */
    public static String anzeige(String text) {
        LocalDate datum = parse(text);
        return datum != null ? text(datum) : text;
    }
}
