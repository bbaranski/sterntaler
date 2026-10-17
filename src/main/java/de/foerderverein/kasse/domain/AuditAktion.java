package de.foerderverein.kasse.domain;

public enum AuditAktion {
    ANGELEGT,
    GEAENDERT,
    STORNIERT,

    /** Wird nicht mehr geschrieben (siehe MitgliedService.loeschen) - bleibt erhalten, damit
     * solche Eintraege aus vorhandenen Datenbanken weiter gelesen werden koennen. */
    GELOESCHT
}
