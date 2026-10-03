package de.foerderverein.kasse.domain;

import java.time.LocalDateTime;

/** Hochgeladener Beleg (Rechnung/Quittung als PDF oder JPG) zu genau einer Buchung - technisch
 * 1:1 ueber buchungId (in der DB als UNIQUE erzwungen), fachlich ist "Ersetzen" eines Belegs ein
 * Overwrite, keine Historie mehrerer Belege je Buchung. Die eigentliche Datei liegt nicht in der
 * Datenbank, sondern im Belege-Verzeichnis neben der .db-Datei (siehe AppConfig.belegeVerzeichnis);
 * speicherpfad ist nur der Dateiname innerhalb dieses Verzeichnisses, kein absoluter Pfad, damit
 * Datenbank und Belege-Verzeichnis gemeinsam verschoben werden koennen, ohne die Zuordnung zu
 * verlieren. */
public class Beleg {

    private Long id;
    private long buchungId;
    private String dateiname;
    private String dateityp;
    private String speicherpfad;
    private LocalDateTime hochgeladenAm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getBuchungId() {
        return buchungId;
    }

    public void setBuchungId(long buchungId) {
        this.buchungId = buchungId;
    }

    public String getDateiname() {
        return dateiname;
    }

    public void setDateiname(String dateiname) {
        this.dateiname = dateiname;
    }

    public String getDateityp() {
        return dateityp;
    }

    public void setDateityp(String dateityp) {
        this.dateityp = dateityp;
    }

    public String getSpeicherpfad() {
        return speicherpfad;
    }

    public void setSpeicherpfad(String speicherpfad) {
        this.speicherpfad = speicherpfad;
    }

    public LocalDateTime getHochgeladenAm() {
        return hochgeladenAm;
    }

    public void setHochgeladenAm(LocalDateTime hochgeladenAm) {
        this.hochgeladenAm = hochgeladenAm;
    }
}
