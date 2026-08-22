package de.foerderverein.kasse.service;

import java.util.List;

import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.KontoRepository;

public class KontoService {

    private final KontoRepository repository;

    public KontoService(KontoRepository repository) {
        this.repository = repository;
    }

    public List<Konto> findAll() {
        return repository.findAll();
    }

    public Konto save(Konto konto) {
        if (konto.getBezeichnung() == null || konto.getBezeichnung().isBlank()) {
            throw new IllegalArgumentException("Bezeichnung darf nicht leer sein");
        }
        if (konto.getIban() == null || konto.getIban().isBlank()) {
            throw new IllegalArgumentException("IBAN darf nicht leer sein");
        }
        konto.setIban(konto.getIban().replace(" ", "").toUpperCase());
        return repository.save(konto);
    }
}
