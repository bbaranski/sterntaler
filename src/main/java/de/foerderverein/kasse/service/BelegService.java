package de.foerderverein.kasse.service;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import de.foerderverein.kasse.domain.Beleg;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.persistence.BelegDateiName;
import de.foerderverein.kasse.persistence.BelegRepository;

public class BelegService {

    private final BelegRepository repository;
    private final GeschaeftsjahrService geschaeftsjahrService;
    private final Path belegeVerzeichnis;

    public BelegService(BelegRepository repository, GeschaeftsjahrService geschaeftsjahrService,
                         Path belegeVerzeichnis) {
        this.repository = repository;
        this.geschaeftsjahrService = geschaeftsjahrService;
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
     * geprueft, da der FileChooser-ExtensionFilter vom Anwender umgangen werden kann. Der
     * Dateiname setzt sich aus Geschaeftsjahr, Rechnungsnummer und Originaldateiname zusammen -
     * dafuer muss die Buchung bereits eine Rechnungsnummer tragen, sonst wird der Upload
     * abgelehnt. Beim Ersetzen mit einem dadurch anderen Dateinamen (z. B. veraenderte
     * Rechnungsnummer oder anderer Dateityp) wird die zuvor gespeicherte Datei entfernt, damit
     * keine verwaisten Dateien im Belege-Verzeichnis zurueckbleiben. */
    public Beleg hochladen(Buchung buchung, File datei) throws IOException {
        String rechnungsnummer = buchung.getRechnungsnummer();
        if (rechnungsnummer == null || rechnungsnummer.isBlank()) {
            throw new IllegalStateException(
                    "Fuer den Beleg-Upload muss zuerst eine Rechnungsnummer zur Buchung erfasst werden.");
        }
        String dateityp = ermittleUndPruefeDateityp(datei);
        String geschaeftsjahr = geschaeftsjahrBezeichnung(buchung);
        String speicherpfad = BelegDateiName.zielname(geschaeftsjahr, rechnungsnummer, datei.getName());

        long buchungId = buchung.getId();
        pruefeKeineKollisionMitAndererBuchung(speicherpfad, buchungId);
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

    /** Entfernt einen vorhandenen Beleg vollstaendig - sowohl die Datei im Belege-Verzeichnis als
     * auch die Datenbankzeile. Ist kein Beleg zur Buchung vorhanden, passiert nichts. */
    public void entfernen(long buchungId) {
        repository.findByBuchungId(buchungId).ifPresent(beleg -> {
            try {
                Files.deleteIfExists(belegeVerzeichnis.resolve(beleg.getSpeicherpfad()));
            } catch (IOException e) {
                throw new UncheckedIOException("Beleg-Datei konnte nicht geloescht werden", e);
            }
            repository.loescheByBuchungId(buchungId);
        });
    }

    private String geschaeftsjahrBezeichnung(Buchung buchung) {
        return geschaeftsjahrService.findeFuer(buchung.getBuchungsdatum(), geschaeftsjahrService.findAll())
                .map(Geschaeftsjahr::getBezeichnung)
                .orElse("ohne-Geschaeftsjahr");
    }

    /** Da sich der Dateiname nun aus Geschaeftsjahr und Rechnungsnummer statt der stets
     * eindeutigen Buchungs-ID ergibt, koennte eine doppelt vergebene Rechnungsnummer sonst den
     * Beleg einer anderen Buchung auf der Festplatte ueberschreiben, ohne dass deren
     * Datenbankzeile davon erfaehrt - das wird hier verhindert. */
    private void pruefeKeineKollisionMitAndererBuchung(String speicherpfad, long buchungId) {
        for (Beleg beleg : repository.findAll().values()) {
            if (speicherpfad.equals(beleg.getSpeicherpfad()) && beleg.getBuchungId() != buchungId) {
                throw new IllegalStateException("Es existiert bereits ein Beleg mit demselben Geschaeftsjahr, "
                        + "derselben Rechnungsnummer und demselben Dateinamen zu einer anderen Buchung - bitte "
                        + "die Rechnungsnummer pruefen.");
            }
        }
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
