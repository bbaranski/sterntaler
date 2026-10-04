package de.foerderverein.kasse.service;

import java.util.List;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.persistence.AktivitaetRepository;

public class AktivitaetService {

    private final AktivitaetRepository repository;

    public AktivitaetService(AktivitaetRepository repository) {
        this.repository = repository;
    }

    public List<Aktivitaet> findAll() {
        return repository.findAll();
    }

        public Aktivitaet save(Aktivitaet aktivitaet) {
        if (aktivitaet.getName() == null || aktivitaet.getName().isBlank()) {
            throw new IllegalArgumentException("Name darf nicht leer sein");
        }
        return repository.save(aktivitaet);
    }

    /** Aktivitaeten werden deaktiviert statt geloescht, damit bestehende Buchungen ihre
     * Zuordnung behalten. */
    public void deaktiviere(Aktivitaet aktivitaet) {
        aktivitaet.setAktiv(false);
        repository.save(aktivitaet);
    }
}
