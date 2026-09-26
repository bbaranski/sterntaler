package de.foerderverein.kasse.domain;

/** Veranstaltung/Aktivitaet, der einzelne Buchungen zugeordnet werden koennen - traegt die
 * steuerliche Sphaere (Bereich) fuer die jaehrliche "Ergaenzung zur Steuererklaerung" (nicht die
 * Kategorie - eine Kategorie wie "Spenden" oder "Vereinsbedarf" beschreibt nur die Art der
 * Ein-/Ausgabe, unabhaengig davon, in welcher Sphaere sie anfaellt). Innerhalb des Wirtschaftlichen
 * Geschaeftsbetriebs entspricht eine Aktivitaet zusaetzlich einer einzelnen Veranstaltung (z. B.
 * "Sankt Martin 2026") fuer die Aufschluesselung je Veranstaltung im Report. Kein Datums-/
 * Geschaeftsjahr-Bezug hier: die Jahresunterscheidung erfolgt ueber den Namen (z. B.
 * "Sankt Martin 2026" vs. "... 2027"), die tatsaechliche Zuordnung zum Steuerjahr laeuft ueber
 * Buchung.buchungsdatum. */
public class Aktivitaet {

    private Long id;
    private String name;
    private String notiz;
    private boolean aktiv = true;
    private Bereich bereich = Bereich.IDEELLER_BEREICH;

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

    public Bereich getBereich() {
        return bereich;
    }

    public void setBereich(Bereich bereich) {
        this.bereich = bereich;
    }

    @Override
    public String toString() {
        return name;
    }
}
