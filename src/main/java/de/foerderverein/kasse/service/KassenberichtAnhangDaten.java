package de.foerderverein.kasse.service;

import java.util.List;

/** Daten des Kassenbericht-Anhangs: dieselbe Gliederung nach Kategorien wie der Kassenbericht,
 * je Kategorie zusaetzlich aufgeschluesselt nach Aktivitaet, und am Ende das Ergebnis je
 * Aktivitaet ueber alle Kategorien hinweg. Alle Betraege in Cent; Ausgaben sind wie im
 * Kassenbericht positive Zahlen. Die Reihenfolge aller Listen ist die des ersten Vorkommens in
 * den nach Buchungsdatum sortierten Buchungen - genau wie die Kategoriereihenfolge des
 * Kassenberichts. Oeffentlich, da BerichtService.berechneKassenberichtAnhang das Ergebnis nach
 * aussen gibt. */
public record KassenberichtAnhangDaten(List<KategorieBlock> einnahmen, List<KategorieBlock> ausgaben,
                                       List<AktivitaetErgebnis> aktivitaeten) {

    /** Eine Kategoriezeile mit ihren Aktivitaetszeilen. aktivitaeten ist leer, wenn keine einzige
     * Buchung dieser Kategorie eine Aktivitaet traegt - eine einzelne Unterzeile
     * "(ohne Aktivitaet)" mit demselben Betrag waere nur Rauschen. Andernfalls summieren sich die
     * Unterzeilen immer auf summeCent, einschliesslich des Restpostens. */
    public record KategorieBlock(String kategorie, long summeCent, List<AktivitaetSumme> aktivitaeten) {
    }

    public record AktivitaetSumme(String aktivitaet, long summeCent) {
    }

    /** Einnahmen und Ausgaben einer Aktivitaet ueber alle Kategorien. Sichtbar wird hier, was die
     * Aufschluesselung je Kategorie nicht zeigen kann: eine Aktivitaet verteilt sich auf mehrere
     * Kategorien und auf beide Abschnitte. */
    public record AktivitaetErgebnis(String aktivitaet, long einnahmenCent, long ausgabenCent) {

        public long ergebnisCent() {
            return einnahmenCent - ausgabenCent;
        }
    }
}
