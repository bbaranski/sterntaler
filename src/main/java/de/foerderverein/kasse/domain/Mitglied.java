package de.foerderverein.kasse.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Mitglied {

    private Long id;
    private Anrede anrede;
    private String vorname;
    private String nachname;
    private String strasse;
    private String plz;
    private String ort;
    private String email;
    private String eintrittsdatum;
    private String austrittsdatum;
    private MitgliedStatus status = MitgliedStatus.AKTIV;
    private String notiz;
    private String iban;
    private String bic;
    private Long mandatsreferenz;
    private String sammellastschrift;
    private Long jahresbeitragCent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Anrede getAnrede() {
        return anrede;
    }

    public void setAnrede(Anrede anrede) {
        this.anrede = anrede;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public String getStrasse() {
        return strasse;
    }

    public void setStrasse(String strasse) {
        this.strasse = strasse;
    }

    public String getPlz() {
        return plz;
    }

    public void setPlz(String plz) {
        this.plz = plz;
    }

    public String getOrt() {
        return ort;
    }

    public void setOrt(String ort) {
        this.ort = ort;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEintrittsdatum() {
        return eintrittsdatum;
    }

    public void setEintrittsdatum(String eintrittsdatum) {
        this.eintrittsdatum = eintrittsdatum;
    }

    public String getAustrittsdatum() {
        return austrittsdatum;
    }

    public void setAustrittsdatum(String austrittsdatum) {
        this.austrittsdatum = austrittsdatum;
    }

    public MitgliedStatus getStatus() {
        return status;
    }

    public void setStatus(MitgliedStatus status) {
        this.status = status;
    }

    public String getNotiz() {
        return notiz;
    }

    public void setNotiz(String notiz) {
        this.notiz = notiz;
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

    public Long getMandatsreferenz() {
        return mandatsreferenz;
    }

    public void setMandatsreferenz(Long mandatsreferenz) {
        this.mandatsreferenz = mandatsreferenz;
    }

    public String getSammellastschrift() {
        return sammellastschrift;
    }

    public void setSammellastschrift(String sammellastschrift) {
        this.sammellastschrift = sammellastschrift;
    }

    public Long getJahresbeitragCent() {
        return jahresbeitragCent;
    }

    public void setJahresbeitragCent(Long jahresbeitragCent) {
        this.jahresbeitragCent = jahresbeitragCent;
    }

    /** Jahresbeitrag in Euro, oder null falls fuer dieses Mitglied nicht hinterlegt. */
    public BigDecimal getJahresbeitragEuro() {
        return jahresbeitragCent != null ? BigDecimal.valueOf(jahresbeitragCent, 2) : null;
    }

    public void setJahresbeitragEuro(BigDecimal jahresbeitragEuro) {
        this.jahresbeitragCent = jahresbeitragEuro != null
                ? jahresbeitragEuro.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
                : null;
    }

    public String getVollerName() {
        return vorname + " " + nachname;
    }

    @Override
    public String toString() {
        return getVollerName();
    }
}
