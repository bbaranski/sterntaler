package de.foerderverein.kasse.service;

import java.util.List;

import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.persistence.KategorieRepository;

public class KategorieService {

    private final KategorieRepository repository;

    public KategorieService(KategorieRepository repository) {
        this.repository = repository;
    }

    public List<Kategorie> findAllAktiv() {
        return repository.findAllAktiv();
    }

    public List<Kategorie> findAll() {
        return repository.findAll();
    }

    public Kategorie save(Kategorie kategorie) {
        if (kategorie.getName() == null || kategorie.getName().isBlank()) {
            throw new IllegalArgumentException("Name darf nicht leer sein");
        }
        if (kategorie.getTyp() == null) {
            throw new IllegalArgumentException("Typ (Einnahme/Ausgabe) muss gesetzt sein");
        }
        return repository.save(kategorie);
    }

    /** Kategorien werden deaktiviert statt geloescht, damit bestehende Buchungen ihre Zuordnung behalten. */
    public void deaktiviere(Kategorie kategorie) {
        kategorie.setAktiv(false);
        repository.save(kategorie);
    }
}
