package de.foerderverein.kasse.domain;

/** Veranstaltung/Aktivitaet (z. B. "Sankt Martin 2026"), der einzelne Buchungen optional
 * zugeordnet werden koennen - rein optionales Reporting-Metadatum ohne jeden Einfluss auf die
 * Steuererklaerung. Die steuerliche Sphaere (Bereich) traegt stattdessen die Kategorie (siehe
 * Kategorie.getBereich()). Kein Datums-/Geschaeftsjahr-Bezug hier: die Jahresunterscheidung
 * erfolgt ueber den Namen (z. B. "Sankt Martin 2026" vs. "... 2027"), die tatsaechliche
 * Zuordnung zum Steuerjahr laeuft ueber Buchung.buchungsdatum. */
public class Aktivitaet {

    private Long id;
    private String name;
    private String notiz;
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

    public String getNotiz() {
        return notiz;
    }

    public void setNotiz(String notiz) {
        this.notiz = notiz;
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
