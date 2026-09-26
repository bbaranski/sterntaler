package de.foerderverein.kasse.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import de.foerderverein.kasse.domain.Beleg;
import de.foerderverein.kasse.persistence.BelegRepository;

public class BelegService {

    private final BelegRepository repository;

    public BelegService(BelegRepository repository) {
        this.repository = repository;
    }

    public Map<Long, Beleg> findAllMetadaten() {
        return repository.findAllMetadaten();
    }

    public Optional<Beleg> findByBuchungId(long buchungId) {
        return repository.findByBuchungId(buchungId);
    }

    /** Laedt die Datei ein und speichert sie als Beleg zur Buchung - legt einen neuen Beleg an
     * oder ersetzt einen bereits vorhandenen (siehe BelegRepository.speichern). Nur PDF und JPG
     * sind zulaessig; die Endung wird serverseitig geprueft, da der FileChooser-ExtensionFilter
     * vom Anwender umgangen werden kann. */
    public Beleg hochladen(long buchungId, File datei) throws IOException {
        String dateityp = ermittleUndPruefeDateityp(datei);
        byte[] inhalt = Files.readAllBytes(datei.toPath());

        Beleg beleg = new Beleg();
        beleg.setBuchungId(buchungId);
        beleg.setDateiname(datei.getName());
        beleg.setDateityp(dateityp);
        beleg.setInhalt(inhalt);
        beleg.setHochgeladenAm(LocalDateTime.now());
        return repository.speichern(beleg);
    }

    private String ermittleUndPruefeDateityp(File datei) {
        String name = datei.getName();
        int punkt = name.lastIndexOf('.');
        String endung = punkt >= 0 ? name.substring(punkt + 1).toLowerCase(Locale.ROOT) : "";
        if (!endung.equals("pdf") && !endung.equals("jpg") && !endung.equals("jpeg")) {
            throw new IllegalArgumentException("Nur PDF- oder JPG-Dateien koennen als Beleg hochgeladen werden.");
        }
        return endung;
    }
}
