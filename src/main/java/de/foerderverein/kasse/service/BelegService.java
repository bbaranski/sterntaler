package de.foerderverein.kasse.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import de.foerderverein.kasse.domain.Beleg;
import de.foerderverein.kasse.persistence.BelegDateiName;
import de.foerderverein.kasse.persistence.BelegRepository;

public class BelegService {

    private final BelegRepository repository;
    private final Path belegeVerzeichnis;

    public BelegService(BelegRepository repository, Path belegeVerzeichnis) {
        this.repository = repository;
        this.belegeVerzeichnis = belegeVerzeichnis;
    }

    public Map<Long, Beleg> findAll() {
        return repository.findAll();
    }

    public Optional<Beleg> findByBuchungId(long buchungId) {
        return repository.findByBuchungId(buchungId);
    }

    public Path pfadZuDatei(Beleg beleg) {
        return belegeVerzeichnis.resolve(beleg.getSpeicherpfad());
    }

    /** Kopiert die Datei ins Belege-Verzeichnis und speichert den Verweis als Beleg zur Buchung -
     * legt einen neuen Beleg an oder ersetzt einen bereits vorhandenen (siehe
     * BelegRepository.speichern). Nur PDF und JPG sind zulaessig; die Endung wird serverseitig
     * geprueft, da der FileChooser-ExtensionFilter vom Anwender umgangen werden kann. Beim
     * Ersetzen mit einem anderen Dateinamen (z. B. anderer Dateityp) wird die zuvor gespeicherte
     * Datei entfernt, damit keine verwaisten Dateien im Belege-Verzeichnis zurueckbleiben. */
    public Beleg hochladen(long buchungId, File datei) throws IOException {
        String dateityp = ermittleUndPruefeDateityp(datei);
        String speicherpfad = BelegDateiName.zielname(buchungId, datei.getName());

        Optional<Beleg> vorhanden = repository.findByBuchungId(buchungId);

        Files.createDirectories(belegeVerzeichnis);
        Files.copy(datei.toPath(), belegeVerzeichnis.resolve(speicherpfad), StandardCopyOption.REPLACE_EXISTING);
        if (vorhanden.isPresent() && !speicherpfad.equals(vorhanden.get().getSpeicherpfad())) {
            Files.deleteIfExists(belegeVerzeichnis.resolve(vorhanden.get().getSpeicherpfad()));
        }

        Beleg beleg = new Beleg();
        beleg.setBuchungId(buchungId);
        beleg.setDateiname(datei.getName());
        beleg.setDateityp(dateityp);
        beleg.setSpeicherpfad(speicherpfad);
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
