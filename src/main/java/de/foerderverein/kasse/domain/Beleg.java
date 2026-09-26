package de.foerderverein.kasse.domain;

import java.time.LocalDateTime;

/** Hochgeladener Beleg (Rechnung/Quittung als PDF oder JPG) zu genau einer Buchung - technisch
 * 1:1 ueber buchungId (in der DB als UNIQUE erzwungen), fachlich ist "Ersetzen" eines Belegs ein
 * Overwrite, keine Historie mehrerer Belege je Buchung. */
public class Beleg {

    private Long id;
    private long buchungId;
    private String dateiname;
    private String dateityp;

    /** Die eigentlichen Bytes des Belegs (BLOB). Nullability-Vertrag: dieses Feld ist nur gefuellt,
     * wenn der Beleg gezielt per BelegRepository.findByBuchungId(...) einzeln geladen wurde (z. B.
     * zum Oeffnen). Bei der Bulk-Metadatenabfrage BelegRepository.findAllMetadaten(), die fuer die
     * Tabellenspalte "Beleg" verwendet wird, bleibt dieses Feld bewusst null, damit das BLOB nicht
     * fuer jede Zeile der Buchungen-Tabelle aus der Datenbank geladen wird. Ein Beleg-Objekt mit
     * inhalt == null ist also weiterhin ein gueltiger "Beleg vorhanden"-Marker - nicht mit "kein
     * Beleg vorhanden" (kein Eintrag in der Map) verwechseln. */
    private byte[] inhalt;

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

    public byte[] getInhalt() {
        return inhalt;
    }

    public void setInhalt(byte[] inhalt) {
        this.inhalt = inhalt;
    }

    public LocalDateTime getHochgeladenAm() {
        return hochgeladenAm;
    }

    public void setHochgeladenAm(LocalDateTime hochgeladenAm) {
        this.hochgeladenAm = hochgeladenAm;
    }
}
