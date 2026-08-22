package de.foerderverein.kasse.service;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import de.foerderverein.kasse.domain.AuditAktion;
import de.foerderverein.kasse.domain.AuditEintrag;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;

public class BuchungService {

    private static final String ENTITY = "Buchung";
    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final NumberFormat BETRAG_FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private final BuchungRepository repository;
    private final GeschaeftsjahrRepository geschaeftsjahrRepository;
    private final AuditLogRepository auditLogRepository;

    public BuchungService(BuchungRepository repository, GeschaeftsjahrRepository geschaeftsjahrRepository,
                           AuditLogRepository auditLogRepository) {
        this.repository = repository;
        this.geschaeftsjahrRepository = geschaeftsjahrRepository;
        this.auditLogRepository = auditLogRepository;
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
        repository.update(buchung);
        protokolliere(buchung, AuditAktion.GEAENDERT, aktuellerBenutzer());
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

    private String aktuellerBenutzer() {
        return System.getProperty("user.name");
    }
}
