package de.foerderverein.kasse.service;

/** Kennzahlen des Mitgliederberichts.
 *
 * <p>mitglieder und die Beitragswerte sind dieselben Groessen, die die Statuszeile der
 * Mitgliederuebersicht anzeigt (siehe MitgliederViewController.aktualisiereStatuszeile), hier
 * ueber die aktiven Mitglieder und als Momentaufnahme zum Erstellungstag - nicht auf das
 * Geschaeftsjahr bezogen. Betraege in Cent, passend zu Mitglied.getJahresbeitragCent().
 * Mitglieder ohne hinterlegten Jahresbeitrag zaehlen bei mitglieder mit, bleiben bei allen
 * Beitragswerten aber unberuecksichtigt - genau wie in der Statuszeile. Hat kein aktives
 * Mitglied einen Jahresbeitrag, sind minCent, maxCent und durchschnittCent null: fuer
 * "kein Wert vorhanden" waere 0 Euro eine falsche Aussage.
 *
 * <p>eintritte und austritte beziehen sich dagegen auf das gewaehlte Geschaeftsjahr und
 * zaehlen ueber alle Mitglieder, unabhaengig vom heutigen Status: Wer im selben Jahr ein- und
 * ausgetreten ist, steht in beiden Zahlen. Grundlage sind die als Text gespeicherten Eintritts-
 * und Austrittsdaten; leere und unlesbare Angaben zaehlen nicht mit, weil sie keinem Jahr
 * zuzuordnen sind (siehe BerichtService.berechneMitgliederbericht).
 *
 * <p>Oeffentlich, da BerichtService.berechneMitgliederbericht das Ergebnis nach aussen gibt. */
public record MitgliederberichtDaten(int mitglieder, long summeCent,
                                     Long minCent, Long maxCent, Long durchschnittCent,
                                     int eintritte, int austritte) {
}
