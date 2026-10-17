package de.foerderverein.kasse.service;

/** Kennzahlen des Mitgliederberichts - dieselben Groessen, die die Statuszeile der
 * Mitgliederuebersicht anzeigt (siehe MitgliederViewController.aktualisiereStatuszeile), hier
 * ueber die aktiven Mitglieder. Betraege in Cent, passend zu Mitglied.getJahresbeitragCent().
 * Mitglieder ohne hinterlegten Jahresbeitrag zaehlen bei mitglieder mit, bleiben bei allen
 * Beitragswerten aber unberuecksichtigt - genau wie in der Statuszeile. Hat kein aktives
 * Mitglied einen Jahresbeitrag, sind minCent, maxCent und durchschnittCent null: fuer
 * "kein Wert vorhanden" waere 0 Euro eine falsche Aussage. Oeffentlich, da
 * BerichtService.berechneMitgliederbericht das Ergebnis nach aussen gibt. */
public record MitgliederberichtDaten(int mitglieder, long summeCent,
                                     Long minCent, Long maxCent, Long durchschnittCent) {
}
