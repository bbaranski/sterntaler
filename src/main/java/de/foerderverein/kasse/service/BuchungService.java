package de.foerderverein.kasse.service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.AuditAktion;
import de.foerderverein.kasse.domain.AuditEintrag;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;

public class BuchungService {

    private static final String ENTITY = "Buchung";
    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private final BuchungRepository repository;
    private final GeschaeftsjahrRepository geschaeftsjahrRepository;
    private final GeschaeftsjahrService geschaeftsjahrService;
    private final AuditLogRepository auditLogRepository;
    private final KategorieRepository kategorieRepository;
    private final AktivitaetRepository aktivitaetRepository;

    public BuchungService(BuchungRepository repository, GeschaeftsjahrRepository geschaeftsjahrRepository,
                           GeschaeftsjahrService geschaeftsjahrService, AuditLogRepository auditLogRepository,
                           KategorieRepository kategorieRepository, AktivitaetRepository aktivitaetRepository) {
        this.repository = repository;
        this.geschaeftsjahrRepository = geschaeftsjahrRepository;
        this.geschaeftsjahrService = geschaeftsjahrService;
        this.auditLogRepository = auditLogRepository;
        this.kategorieRepository = kategorieRepository;
        this.aktivitaetRepository = aktivitaetRepository;
    }

    public List<Buchung> findAll() {
        return repository.findAll();
    }

    public List<AuditEintrag> findVerlauf(long buchungId) {
        return auditLogRepository.findByEntity(ENTITY, buchungId);
    }

    public Buchung erfasseManuell(Buchung buchung, String erstelltVon) {
        validiere(buchung);
        pruefeNichtGesperrt(buchung.getBuchungsdatum());
        buchung.setQuelle(BuchungsQuelle.MANUELL);
        buchung.setErstelltVon(erstelltVon);
        repository.insert(buchung);
        protokolliere(buchung, AuditAktion.ANGELEGT, erstelltVon);
        return buchung;
    }

    public void aktualisiere(Buchung buchung) {
        validiere(buchung);
        pruefeNichtGesperrt(buchung.getBuchungsdatum());
        Map<String, String> altWerte = repository.findById(buchung.getId())
                .map(this::feldwerte)
                .orElseThrow(() -> new IllegalStateException("Buchung nicht gefunden"));
        repository.update(buchung);
        protokolliereAenderung(buchung, altWerte, aktuellerBenutzer());
    }

    public void storniere(Buchung buchung) {
        pruefeNichtGesperrt(buchung.getBuchungsdatum());
        repository.storno(buchung.getId());
        protokolliere(buchung, AuditAktion.STORNIERT, aktuellerBenutzer());
    }

    /** Importiert HBCI-Umsaetze. Bereits vorhandene Buchungen (gleicher Import-Hash am selben Konto)
     * sowie Umsaetze in einem bereits abgeschlossenen Geschaeftsjahr werden uebersprungen.
     * @return Anzahl tatsaechlich importierter Buchungen. */
    public int importiereHbciBuchungen(List<Buchung> vorschlaege) {
        int importiert = 0;
        for (Buchung buchung : vorschlaege) {
            if (buchung.getHbciImportHash() != null
                    && repository.existsByHbciHash(buchung.getKontoId(), buchung.getHbciImportHash())) {
                continue;
            }
            if (geschaeftsjahrRepository.findAbgeschlossenesJahrFuer(buchung.getBuchungsdatum()).isPresent()) {
                continue;
            }
            buchung.setQuelle(BuchungsQuelle.HBCI_IMPORT);
            buchung.setErstelltVon("HBCI-Import");
            repository.insert(buchung);
            protokolliere(buchung, AuditAktion.ANGELEGT, "HBCI-Import");
            importiert++;
        }
        return importiert;
    }

    public boolean existsByHbciHash(long kontoId, String hash) {
        return repository.existsByHbciHash(kontoId, hash);
    }

    private void pruefeNichtGesperrt(LocalDate datum) {
        geschaeftsjahrRepository.findAbgeschlossenesJahrFuer(datum).ifPresent(jahr -> {
            throw new IllegalStateException("Das Geschaeftsjahr \"" + jahr.getBezeichnung()
                    + "\" ist abgeschlossen. Buchungen mit Datum " + datum + " sind gesperrt.");
        });
    }

    private void validiere(Buchung buchung) {
        if (buchung.getKontoId() == null) {
            throw new IllegalArgumentException("Konto muss gesetzt sein");
        }
        if (buchung.getBuchungsdatum() == null) {
            throw new IllegalArgumentException("Buchungsdatum muss gesetzt sein");
        }
        if (buchung.getBetragCent() == 0) {
            throw new IllegalArgumentException("Betrag darf nicht 0 sein");
        }
        pruefeRechnungsnummerEindeutigkeitImGeschaeftsjahr(buchung);
    }

    /** Rechnungsnummern muessen nur innerhalb desselben Geschaeftsjahres eindeutig sein - bei
     * unterschiedlichem Geschaeftsjahr darf dieselbe Nummer erneut vergeben werden (z. B. ein
     * Lieferant, der seine Rechnungsnummerierung jedes Jahr neu beginnt). Buchungen ohne
     * zugehoeriges Geschaeftsjahr (z. B. ausserhalb jedes definierten Zeitraums) werden dabei als
     * eigene, gemeinsame Gruppe behandelt, statt die Pruefung stillschweigend zu uebergehen. */
    private void pruefeRechnungsnummerEindeutigkeitImGeschaeftsjahr(Buchung buchung) {
        String rechnungsnummer = buchung.getRechnungsnummer();
        if (rechnungsnummer == null || rechnungsnummer.isBlank()) {
            return;
        }
        List<Geschaeftsjahr> geschaeftsjahre = geschaeftsjahrService.findAll();
        Long geschaeftsjahrId = geschaeftsjahrIdFuer(buchung.getBuchungsdatum(), geschaeftsjahre);

        for (Buchung andere : repository.findAll()) {
            if (Objects.equals(andere.getId(), buchung.getId())) {
                continue;
            }
            if (!rechnungsnummer.equals(andere.getRechnungsnummer())) {
                continue;
            }
            Long andereGeschaeftsjahrId = geschaeftsjahrIdFuer(andere.getBuchungsdatum(), geschaeftsjahre);
            if (Objects.equals(geschaeftsjahrId, andereGeschaeftsjahrId)) {
                throw new IllegalStateException(
                        "Die Rechnungsnummer \"" + rechnungsnummer + "\" ist in diesem Geschaeftsjahr bereits vergeben.");
            }
        }
    }

    private Long geschaeftsjahrIdFuer(LocalDate datum, List<Geschaeftsjahr> geschaeftsjahre) {
        return geschaeftsjahrService.findeFuer(datum, geschaeftsjahre).map(Geschaeftsjahr::getId).orElse(null);
    }

    private void protokolliere(Buchung buchung, AuditAktion aktion, String benutzer) {
        AuditEintrag eintrag = new AuditEintrag();
        eintrag.setEntity(ENTITY);
        eintrag.setEntityId(buchung.getId());
        eintrag.setAktion(aktion);
        eintrag.setBenutzer(benutzer);
        eintrag.setDetails("Datum: " + buchung.getBuchungsdatum().format(DATUM_FORMAT)
                + ", Betrag: " + BETRAG_FORMAT.format(buchung.getBetragEuro())
                + (buchung.getVerwendungszweck() != null && !buchung.getVerwendungszweck().isBlank()
                        ? ", Verwendungszweck: " + buchung.getVerwendungszweck() : ""));
        auditLogRepository.insert(eintrag);
    }

    /** Erzeugt je tatsaechlich geaendertem Feld einen eigenen Audit-Eintrag (Feld/Wert (alt)/
     * Wert (neu)) statt einer einzelnen zusammenfassenden Textbeschreibung - anders als
     * protokolliere(...), das fuer ANGELEGT/STORNIERT weiterhin verwendet wird, wo eine
     * Feld-fuer-Feld-Aufschluesselung wenig Mehrwert hat (bei der Neuanlage ist ohnehin "alles
     * neu", bei der Stornierung aendert sich technisch nur ein Flag). */
    private void protokolliereAenderung(Buchung buchung, Map<String, String> altWerte, String benutzer) {
        for (Feldvergleich.Aenderung aenderung : Feldvergleich.vergleiche(altWerte, feldwerte(buchung))) {
            AuditEintrag eintrag = new AuditEintrag();
            eintrag.setEntity(ENTITY);
            eintrag.setEntityId(buchung.getId());
            eintrag.setAktion(AuditAktion.GEAENDERT);
            eintrag.setBenutzer(benutzer);
            eintrag.setFeld(aenderung.feld());
            eintrag.setWertAlt(aenderung.wertAlt());
            eintrag.setWertNeu(aenderung.wertNeu());
            auditLogRepository.insert(eintrag);
        }
    }

    /** Menschenlesbarer Schnappschuss der fachlich relevanten, ueber den BuchungDialog
     * editierbaren Felder - Konto ist bewusst ausgenommen, da es bei einer bestehenden Buchung
     * nicht mehr geaendert werden kann (siehe BuchungDialogController). Referenzen auf
     * Kategorie/Aktivitaet werden auf ihren Namen aufgeloest, damit der Verlauf nicht nur IDs
     * zeigt. */
    private LinkedHashMap<String, String> feldwerte(Buchung buchung) {
        LinkedHashMap<String, String> werte = new LinkedHashMap<>();
        werte.put("Datum", buchung.getBuchungsdatum() != null ? buchung.getBuchungsdatum().format(DATUM_FORMAT) : "");
        werte.put("Kontoauszug", wert(buchung.getKontoauszug()));
        werte.put("Betrag", buchung.getBetragEuro() != null ? BETRAG_FORMAT.format(buchung.getBetragEuro()) : "");
        werte.put("Kategorie", kategorieName(buchung.getKategorieId()));
        werte.put("Aktivität", aktivitaetName(buchung.getAktivitaetId()));
        werte.put("Verwendungszweck", wert(buchung.getVerwendungszweck()));
        werte.put("Rechnungsnummer", wert(buchung.getRechnungsnummer()));
        werte.put("Gegenkonto", wert(buchung.getGegenkontoName()));
        werte.put("Gegenkonto IBAN", wert(buchung.getGegenkontoIban()));
        werte.put("Storniert", buchung.isStorniert() ? "Ja" : "Nein");
        return werte;
    }

    private String kategorieName(Long id) {
        return id == null ? "" : kategorieRepository.findById(id).map(Kategorie::getName).orElse("(gelöscht)");
    }

    private String aktivitaetName(Long id) {
        return id == null ? "" : aktivitaetRepository.findById(id).map(Aktivitaet::getName).orElse("(gelöscht)");
    }

    private static String wert(String text) {
        return text != null ? text : "";
    }

    private String aktuellerBenutzer() {
        return System.getProperty("user.name");
    }
}
