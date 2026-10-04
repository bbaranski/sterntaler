package de.foerderverein.kasse.service;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.foerderverein.kasse.domain.Anrede;
import de.foerderverein.kasse.domain.AuditAktion;
import de.foerderverein.kasse.domain.AuditEintrag;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;

public class MitgliedService {

    private static final String ENTITY = "Mitglied";
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private final MitgliedRepository repository;
    private final AuditLogRepository auditLogRepository;

    public MitgliedService(MitgliedRepository repository, AuditLogRepository auditLogRepository) {
        this.repository = repository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<Mitglied> findAll() {
        return repository.findAll();
    }

    public List<Mitglied> findAllAktiv() {
        return repository.findAllAktiv();
    }

    public List<AuditEintrag> findVerlauf(long mitgliedId) {
        return auditLogRepository.findByEntity(ENTITY, mitgliedId);
    }

    public Mitglied save(Mitglied mitglied) {
        if (mitglied.getVorname() == null || mitglied.getVorname().isBlank()) {
            throw new IllegalArgumentException("Vorname darf nicht leer sein");
        }
        if (mitglied.getNachname() == null || mitglied.getNachname().isBlank()) {
            throw new IllegalArgumentException("Nachname darf nicht leer sein");
        }
        if (mitglied.getIban() != null) {
            String normalisiert = mitglied.getIban().replace(" ", "").toUpperCase();
            mitglied.setIban(normalisiert.isBlank() ? null : normalisiert);
        }
        if (mitglied.getBic() != null) {
            String normalisiert = mitglied.getBic().replace(" ", "").toUpperCase();
            mitglied.setBic(normalisiert.isBlank() ? null : normalisiert);
        }
        boolean neu = mitglied.getId() == null;
        if (neu) {
            Mitglied gespeichert = repository.save(mitglied);
            protokolliere(gespeichert, AuditAktion.ANGELEGT, aktuellerBenutzer());
            return gespeichert;
        }
        Map<String, String> altWerte = repository.findById(mitglied.getId())
                .map(this::feldwerte)
                .orElseThrow(() -> new IllegalStateException("Mitglied nicht gefunden"));
        Mitglied gespeichert = repository.save(mitglied);
        protokolliereAenderung(gespeichert, altWerte, aktuellerBenutzer());
        return gespeichert;
    }

    /** Alternative zum endgueltigen Loeschen, falls die Mitgliedschaft (und ihre Historie im
     * Verlauf-Dialog) erhalten bleiben soll - setzt lediglich den Status auf "Inaktiv". */
    public void austreten(Mitglied mitglied) {
        Map<String, String> altWerte = feldwerte(mitglied);
        mitglied.setStatus(MitgliedStatus.INAKTIV);
        repository.save(mitglied);
        protokolliereAenderung(mitglied, altWerte, aktuellerBenutzer());
    }

    /** Loescht ein Mitglied endgueltig - anders als Kategorie/Aktivitaet also kein reines
     * Deaktivieren, sondern ein echtes Entfernen aus der Datenbank. */
    public void loeschen(Mitglied mitglied) {
        repository.loesche(mitglied.getId());
        protokolliere(mitglied, AuditAktion.GELOESCHT, aktuellerBenutzer());
    }

    /** Fuer ANGELEGT/GELOESCHT - ein knapper, menschenlesbarer Schnappschuss statt einzelner
     * Feld-Differenzen (bei Neuanlage/Loeschung ist ohnehin "alles neu"/"alles weg", eine
     * Feld-fuer-Feld-Aufschluesselung bringt dort wenig). */
    private void protokolliere(Mitglied mitglied, AuditAktion aktion, String benutzer) {
        AuditEintrag eintrag = new AuditEintrag();
        eintrag.setEntity(ENTITY);
        eintrag.setEntityId(mitglied.getId());
        eintrag.setAktion(aktion);
        eintrag.setBenutzer(benutzer);
        eintrag.setDetails("Name: " + mitglied.getVollerName() + ", Status: " + mitglied.getStatus().name()
                + (mitglied.getEmail() != null && !mitglied.getEmail().isBlank()
                        ? ", E-Mail: " + mitglied.getEmail() : ""));
        auditLogRepository.insert(eintrag);
    }

    /** Erzeugt je tatsaechlich geaendertem Feld einen eigenen Audit-Eintrag (Feld/Wert (alt)/
     * Wert (neu)) - siehe BuchungService.protokolliereAenderung(...) fuer das Gegenstueck. */
    private void protokolliereAenderung(Mitglied mitglied, Map<String, String> altWerte, String benutzer) {
        for (Feldvergleich.Aenderung aenderung : Feldvergleich.vergleiche(altWerte, feldwerte(mitglied))) {
            AuditEintrag eintrag = new AuditEintrag();
            eintrag.setEntity(ENTITY);
            eintrag.setEntityId(mitglied.getId());
            eintrag.setAktion(AuditAktion.GEAENDERT);
            eintrag.setBenutzer(benutzer);
            eintrag.setFeld(aenderung.feld());
            eintrag.setWertAlt(aenderung.wertAlt());
            eintrag.setWertNeu(aenderung.wertNeu());
            auditLogRepository.insert(eintrag);
        }
    }

    /** Menschenlesbarer Schnappschuss aller ueber den MitgliedDialog editierbaren Felder, in der
     * dort verwendeten Reihenfolge. */
    private LinkedHashMap<String, String> feldwerte(Mitglied mitglied) {
        LinkedHashMap<String, String> werte = new LinkedHashMap<>();
        werte.put("Anrede", anredeText(mitglied.getAnrede()));
        werte.put("Vorname", wert(mitglied.getVorname()));
        werte.put("Nachname", wert(mitglied.getNachname()));
        werte.put("Straße", wert(mitglied.getStrasse()));
        werte.put("PLZ", wert(mitglied.getPlz()));
        werte.put("Ort", wert(mitglied.getOrt()));
        werte.put("Telefon", wert(mitglied.getTelefon()));
        werte.put("E-Mail", wert(mitglied.getEmail()));
        werte.put("Eintrittsdatum", wert(mitglied.getEintrittsdatum()));
        werte.put("Austrittsdatum", wert(mitglied.getAustrittsdatum()));
        werte.put("Status", mitglied.getStatus() != null ? mitglied.getStatus().name() : "");
        werte.put("Jahresbeitrag", mitglied.getJahresbeitragEuro() != null
                ? BETRAG_FORMAT.format(mitglied.getJahresbeitragEuro()) : "");
        werte.put("IBAN", wert(mitglied.getIban()));
        werte.put("BIC", wert(mitglied.getBic()));
        werte.put("Mandatsreferenz", mitglied.getMandatsreferenz() != null
                ? String.valueOf(mitglied.getMandatsreferenz()) : "");
        werte.put("Sammellastschrift", wert(mitglied.getSammellastschrift()));
        werte.put("Notiz", wert(mitglied.getNotiz()));
        return werte;
    }

    private static String anredeText(Anrede anrede) {
        if (anrede == null) {
            return "";
        }
        return switch (anrede) {
            case HERR -> "Herr";
            case FRAU -> "Frau";
            case FAMILIE -> "Familie";
        };
    }

    private static String wert(String text) {
        return text != null ? text : "";
    }

    private String aktuellerBenutzer() {
        return System.getProperty("user.name");
    }
}
