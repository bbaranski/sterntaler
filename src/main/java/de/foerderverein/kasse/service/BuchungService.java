package de.foerderverein.kasse.service;

import java.time.LocalDate;
import java.util.List;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;

public class BuchungService {

    private final BuchungRepository repository;
    private final GeschaeftsjahrRepository geschaeftsjahrRepository;

    public BuchungService(BuchungRepository repository, GeschaeftsjahrRepository geschaeftsjahrRepository) {
        this.repository = repository;
        this.geschaeftsjahrRepository = geschaeftsjahrRepository;
    }

    public List<Buchung> findAll() {
        return repository.findAll();
    }

    public Buchung erfasseManuell(Buchung buchung, String erstelltVon) {
        validiere(buchung);
        pruefeNichtGesperrt(buchung.getBuchungsdatum());
        buchung.setQuelle(BuchungsQuelle.MANUELL);
        buchung.setErstelltVon(erstelltVon);
        return repository.insert(buchung);
    }

    public void aktualisiere(Buchung buchung) {
        validiere(buchung);
        pruefeNichtGesperrt(buchung.getBuchungsdatum());
        repository.update(buchung);
    }

    public void storniere(Buchung buchung) {
        pruefeNichtGesperrt(buchung.getBuchungsdatum());
        repository.storno(buchung.getId());
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
}
