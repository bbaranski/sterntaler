package de.foerderverein.kasse.service;

import java.util.List;

/** Ergebnis von BerichtService.importiereMitgliederliste: Anzahl erfolgreich angelegter
 * Mitglieder sowie eine Fehlermeldung je uebersprungener Zeile (fehlerhafte Zeilen brechen den
 * Import nicht ab, siehe importiereMitgliederliste). Oeffentlich, da die UI-Schicht (MainController)
 * das Ergebnis zur Anzeige einer Zusammenfassung auswertet. */
public record MitgliederImportErgebnis(int importiert, List<String> fehler) {
}
