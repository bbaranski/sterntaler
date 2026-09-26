package de.foerderverein.kasse.service;

import java.util.List;

import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;

public class MitgliedService {

    private final MitgliedRepository repository;
    private final BuchungRepository buchungRepository;

    public MitgliedService(MitgliedRepository repository, BuchungRepository buchungRepository) {
        this.repository = repository;
        this.buchungRepository = buchungRepository;
    }

    public List<Mitglied> findAll() {
        return repository.findAll();
    }

    public List<Mitglied> findAllAktiv() {
        return repository.findAllAktiv();
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
        return repository.save(mitglied);
    }

    /** Mitglieder werden auf inaktiv gesetzt statt geloescht, damit bestehende Buchungszuordnungen
     * (informative Verknuepfung, siehe Buchung.mitgliedId) nachvollziehbar bleiben. */
    public void austreten(Mitglied mitglied) {
        mitglied.setStatus(MitgliedStatus.INAKTIV);
        repository.save(mitglied);
    }

    /** Loescht ein Mitglied endgueltig - anders als Kategorie/Aktivitaet also kein reines
     * Deaktivieren, sondern ein echtes Entfernen aus der Datenbank. Nur erlaubt, wenn dem
     * Mitglied keine Buchung zugeordnet ist (siehe Buchung.mitgliedId); ist es zugeordnet, soll
     * stattdessen ueber austreten() der Status auf "Inaktiv" gesetzt werden, damit die
     * Buchungshistorie nachvollziehbar bleibt. */
    public void loeschen(Mitglied mitglied) {
        if (buchungRepository.existsByMitgliedId(mitglied.getId())) {
            throw new IllegalStateException(
                    "Mitglied kann nicht gelöscht werden, da Buchungen zugeordnet sind. "
                            + "Bitte stattdessen den Status auf \"Inaktiv\" setzen.");
        }
        repository.loesche(mitglied.getId());
    }
}
