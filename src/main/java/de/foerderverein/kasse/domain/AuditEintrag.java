package de.foerderverein.kasse.domain;

import java.time.LocalDateTime;

public class AuditEintrag {

    private Long id;
    private String entity;
    private long entityId;
    private AuditAktion aktion;
    private String details;
    private String feld;
    private String wertAlt;
    private String wertNeu;
    private String benutzer;
    private LocalDateTime zeitstempel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public AuditAktion getAktion() {
        return aktion;
    }

    public void setAktion(AuditAktion aktion) {
        this.aktion = aktion;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getFeld() {
        return feld;
    }

    public void setFeld(String feld) {
        this.feld = feld;
    }

    public String getWertAlt() {
        return wertAlt;
    }

    public void setWertAlt(String wertAlt) {
        this.wertAlt = wertAlt;
    }

    public String getWertNeu() {
        return wertNeu;
    }

    public void setWertNeu(String wertNeu) {
        this.wertNeu = wertNeu;
    }

    public String getBenutzer() {
        return benutzer;
    }

    public void setBenutzer(String benutzer) {
        this.benutzer = benutzer;
    }

    public LocalDateTime getZeitstempel() {
        return zeitstempel;
    }

    public void setZeitstempel(LocalDateTime zeitstempel) {
        this.zeitstempel = zeitstempel;
    }
}
