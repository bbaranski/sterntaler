package de.foerderverein.kasse.persistence;

/** Erzeugt aus Buchungs-ID und urspruenglichem Dateinamen einen stabilen, dateisystemsicheren
 * Zielnamen fuer eine Beleg-Datei im Belege-Verzeichnis (siehe AppConfig.belegeVerzeichnis) - die
 * Buchungs-ID macht den Namen eindeutig (ein Beleg pro Buchung), der Originalname bleibt beim
 * Durchsuchen des Ordners erkennbar. Wird sowohl beim regulaeren Hochladen
 * (BelegService.hochladen) als auch bei der einmaligen Migration bestehender BLOBs
 * (BelegDateiMigration) verwendet, damit beide Pfade exakt denselben Namen erzeugen. */
public final class BelegDateiName {

    private BelegDateiName() {
    }

    public static String zielname(long buchungId, String originalDateiname) {
        String bereinigt = originalDateiname.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return buchungId + "_" + bereinigt;
    }
}
