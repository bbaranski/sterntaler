package de.foerderverein.kasse.domain;

/** Wiederverwendbare Serienbrief-Vorlage fuer einen Anlass (z. B. "Vorstandssitzung",
 * "Jahreshauptversammlung") - verweist auf eine vom Nutzer selbst gestaltete Word-Datei
 * (.docx) mit Platzhaltern (siehe SerienbriefService), die im Vorlagen-Verzeichnis neben der
 * .db-Datei liegt; speicherpfad ist nur der Dateiname darin, kein absoluter Pfad (gleiches
 * Prinzip wie bei Beleg). brieftext/signaturName/signaturRolle sind Vorschlagswerte, die beim
 * Erzeugen eines Serienbriefs vorausgefuellt, aber noch angepasst werden koennen. */
public class Briefvorlage {

    private Long id;
    private String name;
    private String betreff;
    private String dateiname;
    private String speicherpfad;
    private String brieftext;
    private String signaturName;
    private String signaturRolle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBetreff() {
        return betreff;
    }

    public void setBetreff(String betreff) {
        this.betreff = betreff;
    }

    public String getDateiname() {
        return dateiname;
    }

    public void setDateiname(String dateiname) {
        this.dateiname = dateiname;
    }

    public String getSpeicherpfad() {
        return speicherpfad;
    }

    public void setSpeicherpfad(String speicherpfad) {
        this.speicherpfad = speicherpfad;
    }

    public String getBrieftext() {
        return brieftext;
    }

    public void setBrieftext(String brieftext) {
        this.brieftext = brieftext;
    }

    public String getSignaturName() {
        return signaturName;
    }

    public void setSignaturName(String signaturName) {
        this.signaturName = signaturName;
    }

    public String getSignaturRolle() {
        return signaturRolle;
    }

    public void setSignaturRolle(String signaturRolle) {
        this.signaturRolle = signaturRolle;
    }

    @Override
    public String toString() {
        return name;
    }
}
