package de.foerderverein.kasse.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Buchung {

    private Long id;
    private Long kontoId;
    private Long kategorieId;
    private Long mitgliedId;
    private Long aktivitaetId;
    private LocalDate buchungsdatum;
    private LocalDate wertstellung;
    private long betragCent;
    private String waehrung = "EUR";
    private String verwendungszweck;
    private String gegenkontoIban;
    private String gegenkontoName;
    private BuchungsQuelle quelle = BuchungsQuelle.MANUELL;
    private String hbciImportHash;
    private boolean storniert;
    private Long stornoReferenzId;
    private String erstelltVon;
    private LocalDateTime erstelltAm;
    private LocalDateTime geaendertAm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKontoId() {
        return kontoId;
    }

    public void setKontoId(Long kontoId) {
        this.kontoId = kontoId;
    }

    public Long getKategorieId() {
        return kategorieId;
    }

    public void setKategorieId(Long kategorieId) {
        this.kategorieId = kategorieId;
    }

    public Long getMitgliedId() {
        return mitgliedId;
    }

    public void setMitgliedId(Long mitgliedId) {
        this.mitgliedId = mitgliedId;
    }

    public Long getAktivitaetId() {
        return aktivitaetId;
    }

    public void setAktivitaetId(Long aktivitaetId) {
        this.aktivitaetId = aktivitaetId;
    }

    public LocalDate getBuchungsdatum() {
        return buchungsdatum;
    }

    public void setBuchungsdatum(LocalDate buchungsdatum) {
        this.buchungsdatum = buchungsdatum;
    }

    public LocalDate getWertstellung() {
        return wertstellung;
    }

    public void setWertstellung(LocalDate wertstellung) {
        this.wertstellung = wertstellung;
    }

    public long getBetragCent() {
        return betragCent;
    }

    public void setBetragCent(long betragCent) {
        this.betragCent = betragCent;
    }

    /** Betrag in Euro, kaufmaennisch gerundet auf 2 Nachkommastellen. */
    public BigDecimal getBetragEuro() {
        return BigDecimal.valueOf(betragCent, 2);
    }

    public void setBetragEuro(BigDecimal betrag) {
        this.betragCent = betrag.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    public String getWaehrung() {
        return waehrung;
    }

    public void setWaehrung(String waehrung) {
        this.waehrung = waehrung;
    }

    public String getVerwendungszweck() {
        return verwendungszweck;
    }

    public void setVerwendungszweck(String verwendungszweck) {
        this.verwendungszweck = verwendungszweck;
    }

    public String getGegenkontoIban() {
        return gegenkontoIban;
    }

    public void setGegenkontoIban(String gegenkontoIban) {
        this.gegenkontoIban = gegenkontoIban;
    }

    public String getGegenkontoName() {
        return gegenkontoName;
    }

    public void setGegenkontoName(String gegenkontoName) {
        this.gegenkontoName = gegenkontoName;
    }

    public BuchungsQuelle getQuelle() {
        return quelle;
    }

    public void setQuelle(BuchungsQuelle quelle) {
        this.quelle = quelle;
    }

    public String getHbciImportHash() {
        return hbciImportHash;
    }

    public void setHbciImportHash(String hbciImportHash) {
        this.hbciImportHash = hbciImportHash;
    }

    public boolean isStorniert() {
        return storniert;
    }

    public void setStorniert(boolean storniert) {
        this.storniert = storniert;
    }

    public Long getStornoReferenzId() {
        return stornoReferenzId;
    }

    public void setStornoReferenzId(Long stornoReferenzId) {
        this.stornoReferenzId = stornoReferenzId;
    }

    public String getErstelltVon() {
        return erstelltVon;
    }

    public void setErstelltVon(String erstelltVon) {
        this.erstelltVon = erstelltVon;
    }

    public LocalDateTime getErstelltAm() {
        return erstelltAm;
    }

    public void setErstelltAm(LocalDateTime erstelltAm) {
        this.erstelltAm = erstelltAm;
    }

    public LocalDateTime getGeaendertAm() {
        return geaendertAm;
    }

    public void setGeaendertAm(LocalDateTime geaendertAm) {
        this.geaendertAm = geaendertAm;
    }
}
