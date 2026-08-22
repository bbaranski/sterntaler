package de.foerderverein.kasse.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class Geschaeftsjahr {

    private Long id;
    private String bezeichnung;
    private LocalDate startDatum;
    private LocalDate endDatum;
    private long startsaldoCent;
    private boolean abgeschlossen;
    private LocalDate abschlussdatum;
    private LocalDate kassenpruefungDatum;
    private String kassenpruefer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    public LocalDate getStartDatum() {
        return startDatum;
    }

    public void setStartDatum(LocalDate startDatum) {
        this.startDatum = startDatum;
    }

    public LocalDate getEndDatum() {
        return endDatum;
    }

    public void setEndDatum(LocalDate endDatum) {
        this.endDatum = endDatum;
    }

    public long getStartsaldoCent() {
        return startsaldoCent;
    }

    public void setStartsaldoCent(long startsaldoCent) {
        this.startsaldoCent = startsaldoCent;
    }

    /** Startsaldo in Euro, kaufmaennisch gerundet auf 2 Nachkommastellen. */
    public BigDecimal getStartsaldoEuro() {
        return BigDecimal.valueOf(startsaldoCent, 2);
    }

    public void setStartsaldoEuro(BigDecimal startsaldo) {
        this.startsaldoCent = startsaldo.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    public boolean isAbgeschlossen() {
        return abgeschlossen;
    }

    public void setAbgeschlossen(boolean abgeschlossen) {
        this.abgeschlossen = abgeschlossen;
    }

    public LocalDate getAbschlussdatum() {
        return abschlussdatum;
    }

    public void setAbschlussdatum(LocalDate abschlussdatum) {
        this.abschlussdatum = abschlussdatum;
    }

    public LocalDate getKassenpruefungDatum() {
        return kassenpruefungDatum;
    }

    public void setKassenpruefungDatum(LocalDate kassenpruefungDatum) {
        this.kassenpruefungDatum = kassenpruefungDatum;
    }

    public String getKassenpruefer() {
        return kassenpruefer;
    }

    public void setKassenpruefer(String kassenpruefer) {
        this.kassenpruefer = kassenpruefer;
    }

    public boolean enthaelt(LocalDate datum) {
        return !datum.isBefore(startDatum) && !datum.isAfter(endDatum);
    }

    @Override
    public String toString() {
        return bezeichnung;
    }
}
