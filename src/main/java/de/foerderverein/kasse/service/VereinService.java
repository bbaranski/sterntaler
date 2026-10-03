package de.foerderverein.kasse.service;

import java.util.Optional;

import de.foerderverein.kasse.domain.Verein;
import de.foerderverein.kasse.persistence.VereinRepository;

public class VereinService {

    private final VereinRepository repository;

    public VereinService(VereinRepository repository) {
        this.repository = repository;
    }

    public Optional<Verein> find() {
        return repository.find();
    }

    public Verein save(Verein verein) {
        if (verein.getName() == null || verein.getName().isBlank()) {
            throw new IllegalArgumentException("Name darf nicht leer sein");
        }
        if (verein.getIban() != null) {
            String normalisiert = verein.getIban().replace(" ", "").toUpperCase();
            verein.setIban(normalisiert.isBlank() ? null : normalisiert);
        }
        if (verein.getBic() != null) {
            String normalisiert = verein.getBic().replace(" ", "").toUpperCase();
            verein.setBic(normalisiert.isBlank() ? null : normalisiert);
        }
        return repository.save(verein);
    }
}
