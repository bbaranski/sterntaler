package de.foerderverein.kasse.service;

import java.util.Map;

/** Fertig aggregierte Zahlen fuer die "Ergaenzung zur Steuererklaerung", getrennt von der
 * PDF-Erzeugung gehalten, damit die Berechnung ohne PDF-Rendering testbar ist (siehe
 * BerichtService.berechneSteuerreport). */
record SteuerreportDaten(
        Map<String, Long> einnahmenProKategorieIdeell,
        Map<String, Long> ausgabenProKategorieIdeell,
        Map<String, Long> einnahmenProKategorieVermoegen,
        Map<String, Long> ausgabenProKategorieVermoegen,
        Map<String, Long> einnahmenProKategorieZweckbetrieb,
        Map<String, Long> ausgabenProKategorieZweckbetrieb,
        Map<String, AktivitaetSumme> summenProAktivitaetGeschaeftsbetrieb) {
}
