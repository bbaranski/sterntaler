package de.foerderverein.kasse.persistence;

/** Erzeugt aus Geschaeftsjahr, Rechnungsnummer und urspruenglichem Dateinamen einen
 * dateisystemsicheren Zielnamen fuer eine Beleg-Datei im Belege-Verzeichnis (siehe
 * AppConfig.belegeVerzeichnis) - entspricht der ueblichen Ablage von Belegen nach Geschaeftsjahr
 * und Rechnungsnummer, der Originalname bleibt beim Durchsuchen des Ordners zusaetzlich
 * erkennbar. */
public final class BelegDateiName {

    private BelegDateiName() {
    }

    public static String zielname(String geschaeftsjahr, String rechnungsnummer, String originalDateiname) {
        return bereinige(geschaeftsjahr) + "_" + bereinige(rechnungsnummer) + "_" + bereinige(originalDateiname);
    }

    private static String bereinige(String wert) {
        return wert.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
