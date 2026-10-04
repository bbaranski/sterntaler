package de.foerderverein.kasse.domain;

import java.time.LocalDate;

public class Konto {

    private Long id;
    private String bezeichnung;
    private String iban;
    private String bic;
    private String bankname;
    private LocalDate saldoStandDatum;
    private String hbciBlz;
    private String hbciUserId;
    private String hbciCustomerId;

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

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public String getBankname() {
        return bankname;
    }

    public void setBankname(String bankname) {
        this.bankname = bankname;
    }

    public LocalDate getSaldoStandDatum() {
        return saldoStandDatum;
    }

    public void setSaldoStandDatum(LocalDate saldoStandDatum) {
        this.saldoStandDatum = saldoStandDatum;
    }

    public String getHbciBlz() {
        return hbciBlz;
    }

    public void setHbciBlz(String hbciBlz) {
        this.hbciBlz = hbciBlz;
    }

    public String getHbciUserId() {
        return hbciUserId;
    }

    public void setHbciUserId(String hbciUserId) {
        this.hbciUserId = hbciUserId;
    }

    public String getHbciCustomerId() {
        return hbciCustomerId;
    }

    public void setHbciCustomerId(String hbciCustomerId) {
        this.hbciCustomerId = hbciCustomerId;
    }

    public boolean isHbciEingerichtet() {
        return hbciBlz != null && !hbciBlz.isBlank() && hbciUserId != null && !hbciUserId.isBlank();
    }

    @Override
    public String toString() {
        return bezeichnung != null ? bezeichnung : iban;
    }
}
