package de.foerderverein.kasse.service;

import java.time.LocalDate;
import java.util.List;

/** Daten des Buchungsjournals: alle Buchungen eines Geschaeftsjahres in derselben Reihenfolge und
 * mit demselben laufenden Saldo wie die Buchungsuebersicht. Betraege in Cent. Die Buchungsmenge
 * entspricht der des Kassenberichts (ohne stornierte, ohne Nullbuchungen), damit sich die Summen
 * beider Dokumente direkt vergleichen lassen. Oeffentlich, da
 * BerichtService.berechneBuchungsjournal das Ergebnis nach aussen gibt. */
public record BuchungsjournalDaten(List<Zeile> zeilen, long startsaldoCent, long summeCent, long endsaldoCent) {

    /** kategorie und aktivitaet sind leer, wenn keine zugeordnet ist - anders als in den
     * Summenberichten bewusst ohne Sammelposten "(ohne Kategorie)"/"(ohne Aktivitaet)": in einer
     * Einzelaufstellung waeren die vielen Wiederholungen nur Rauschen. */
    public record Zeile(LocalDate datum, String kontoauszug, String rechnungsnummer, long betragCent,
                        long saldoCent, String kategorie, String aktivitaet, String verwendungszweck) {
    }
}
