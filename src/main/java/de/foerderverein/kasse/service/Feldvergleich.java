package de.foerderverein.kasse.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Vergleicht zwei geordnete Feld-Wert-Schnappschuesse eines Objekts (vor/nach einer Aenderung)
 * und liefert nur die tatsaechlich unterschiedlichen Felder - Grundlage fuer die
 * "Feld"/"Wert (alt)"/"Wert (neu)"-Spalten im Verlauf-Dialog (siehe BuchungService/MitgliedService). */
final class Feldvergleich {

    record Aenderung(String feld, String wertAlt, String wertNeu) {
    }

    private Feldvergleich() {
    }

    static List<Aenderung> vergleiche(Map<String, String> alt, Map<String, String> neu) {
        List<Aenderung> ergebnis = new ArrayList<>();
        for (Map.Entry<String, String> entry : neu.entrySet()) {
            String wertAlt = alt.get(entry.getKey());
            String wertNeu = entry.getValue();
            if (!Objects.equals(wertAlt, wertNeu)) {
                ergebnis.add(new Aenderung(entry.getKey(), wertAlt, wertNeu));
            }
        }
        return ergebnis;
    }
}
