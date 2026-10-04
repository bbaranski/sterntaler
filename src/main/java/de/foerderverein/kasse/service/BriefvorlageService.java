package de.foerderverein.kasse.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;

import de.foerderverein.kasse.domain.Briefvorlage;
import de.foerderverein.kasse.persistence.BriefvorlageRepository;
import de.foerderverein.kasse.persistence.VorlageDateiName;

public class BriefvorlageService {

    private final BriefvorlageRepository repository;
    private final Path vorlagenVerzeichnis;

    public BriefvorlageService(BriefvorlageRepository repository, Path vorlagenVerzeichnis) {
        this.repository = repository;
        this.vorlagenVerzeichnis = vorlagenVerzeichnis;
    }

    public List<Briefvorlage> findAll() {
        return repository.findAll();
    }

    public Path pfadZuDatei(Briefvorlage vorlage) {
        return vorlagenVerzeichnis.resolve(vorlage.getSpeicherpfad());
    }

    /** Speichert eine Vorlage. Wird eine neue Word-Datei uebergeben (Hochladen/Ersetzen), wird sie
     * geprueft (.docx-Endung) und ins Vorlagen-Verzeichnis kopiert; die zuvor hinterlegte Datei
     * wird dabei entfernt, falls sich der Zieldateiname aendert. Ohne neue Datei bleiben
     * dateiname/speicherpfad unveraendert (reines Bearbeiten von Betreff/Brieftext/Signatur). Eine
     * neue Vorlage ohne jemals hochgeladene Datei ist unzulaessig. */
    public Briefvorlage speichern(Briefvorlage vorlage, File neueDocxDateiOderNull) throws IOException {
        validiere(vorlage);
        if (neueDocxDateiOderNull != null) {
            ladeDateiHoch(vorlage, neueDocxDateiOderNull);
        } else if (vorlage.getSpeicherpfad() == null) {
            throw new IllegalArgumentException("Bitte zuerst eine Word-Datei (.docx) auswaehlen.");
        }
        return repository.speichern(vorlage);
    }

    public void entfernen(Briefvorlage vorlage) {
        try {
            Files.deleteIfExists(pfadZuDatei(vorlage));
        } catch (IOException e) {
            throw new UncheckedIOException("Vorlagen-Datei konnte nicht geloescht werden", e);
        }
        repository.loeschen(vorlage.getId());
    }

    private void ladeDateiHoch(Briefvorlage vorlage, File datei) throws IOException {
        String name = datei.getName();
        int punkt = name.lastIndexOf('.');
        String endung = punkt >= 0 ? name.substring(punkt + 1).toLowerCase(Locale.ROOT) : "";
        if (!endung.equals("docx")) {
            throw new IllegalArgumentException("Nur Word-Dateien (.docx) koennen als Vorlage hochgeladen werden.");
        }

        String alterSpeicherpfad = vorlage.getSpeicherpfad();
        String speicherpfad = VorlageDateiName.zielname(vorlage.getName(), name);

        Files.createDirectories(vorlagenVerzeichnis);
        Files.copy(datei.toPath(), vorlagenVerzeichnis.resolve(speicherpfad), StandardCopyOption.REPLACE_EXISTING);
        if (alterSpeicherpfad != null && !alterSpeicherpfad.equals(speicherpfad)) {
            Files.deleteIfExists(vorlagenVerzeichnis.resolve(alterSpeicherpfad));
        }

        vorlage.setDateiname(name);
        vorlage.setSpeicherpfad(speicherpfad);
    }

    private void validiere(Briefvorlage vorlage) {
        if (vorlage.getName() == null || vorlage.getName().isBlank()) {
            throw new IllegalArgumentException("Name muss gesetzt sein");
        }
        if (vorlage.getBetreff() == null || vorlage.getBetreff().isBlank()) {
            throw new IllegalArgumentException("Betreff muss gesetzt sein");
        }
        if (vorlage.getBrieftext() == null || vorlage.getBrieftext().isBlank()) {
            throw new IllegalArgumentException("Brieftext muss gesetzt sein");
        }
    }
}
