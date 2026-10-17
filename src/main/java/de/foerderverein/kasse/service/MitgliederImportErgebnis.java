package de.foerderverein.kasse.service;

import java.util.List;

/** Ergebnis von BerichtService.importiereMitgliederliste. Der Import erfolgt ganz oder gar
 * nicht: ist fehler nicht leer, wurde nichts geschrieben und importiert ist 0; andernfalls ist
 * fehler leer und importiert nennt die Anzahl der angelegten Mitglieder. Eine Zeile kann mehrere
 * Meldungen beitragen, damit sich alle Beanstandungen in einem Durchgang beheben lassen.
 * Oeffentlich, da die UI-Schicht (MainController) das Ergebnis zur Anzeige auswertet. */
public record MitgliederImportErgebnis(int importiert, List<String> fehler) {
}
