package de.foerderverein.kasse.domain;

public class Kategorie {

    private Long id;
    private String name;
    private Bereich bereich = Bereich.IDEELLER_BEREICH;
    private Long uebergeordneteKategorieId;
    private boolean aktiv = true;

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

    public Bereich getBereich() {
        return bereich;
    }

    public void setBereich(Bereich bereich) {
        this.bereich = bereich;
    }

    public Long getUebergeordneteKategorieId() {
        return uebergeordneteKategorieId;
    }

    public void setUebergeordneteKategorieId(Long uebergeordneteKategorieId) {
        this.uebergeordneteKategorieId = uebergeordneteKategorieId;
    }

    public boolean isAktiv() {
        return aktiv;
    }

    public void setAktiv(boolean aktiv) {
        this.aktiv = aktiv;
    }

    @Override
    public String toString() {
        return name;
    }
}
