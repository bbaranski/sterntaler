package de.foerderverein.kasse.persistence;

/** Erzeugt aus dem Vorlagennamen und dem urspruenglichen Dateinamen einen dateisystemsicheren
 * Zielnamen fuer eine Serienbriefvorlagen-Datei im Vorlagen-Verzeichnis (siehe
 * AppConfig.vorlagenVerzeichnis) - der Vorlagenname macht den Namen eindeutig genug fuer diese
 * uebersichtliche Anzahl an Vorlagen, der Originalname bleibt beim Durchsuchen des Ordners
 * erkennbar. Gleiches Prinzip wie BelegDateiName. */
public final class VorlageDateiName {

    private VorlageDateiName() {
    }

    public static String zielname(String vorlagenname, String originalDateiname) {
        return bereinige(vorlagenname) + "_" + bereinige(originalDateiname);
    }

    private static String bereinige(String wert) {
        return wert.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
}
