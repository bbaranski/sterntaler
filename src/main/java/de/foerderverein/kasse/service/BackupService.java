package de.foerderverein.kasse.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;

/** Datensicherung: eine konsistente Kopie der Datenbankdatei sowie ein CSV-Export aller
 * Buchungen als formatunabhaengige Zweitsicherung, falls die SQLite-Datei einmal nicht
 * mehr lesbar sein sollte. */
public class BackupService {

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final Database database;
    private final BuchungRepository buchungRepository;
    private final KontoRepository kontoRepository;
    private final KategorieRepository kategorieRepository;
    private final MitgliedRepository mitgliedRepository;

    public BackupService(Database database, BuchungRepository buchungRepository, KontoRepository kontoRepository,
                          KategorieRepository kategorieRepository, MitgliedRepository mitgliedRepository) {
        this.database = database;
        this.buchungRepository = buchungRepository;
        this.kontoRepository = kontoRepository;
        this.kategorieRepository = kategorieRepository;
        this.mitgliedRepository = mitgliedRepository;
    }

    /** Erstellt eine konsistente Kopie der Datenbank am Zielpfad. Nutzt SQLite's VACUUM INTO
     * statt eines reinen Datei-Kopiervorgangs, damit die Sicherung auch bei einer (theoretisch)
     * gerade offenen Transaktion konsistent ist, und komprimiert dabei nebenbei die Datei. */
    public void sichereDatenbank(Path ziel) throws IOException {
        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        Files.deleteIfExists(ziel);
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement("VACUUM INTO ?")) {
            ps.setString(1, ziel.toAbsolutePath().toString());
            ps.execute();
        } catch (SQLException e) {
            throw new IOException("Datenbank-Sicherung konnte nicht erstellt werden", e);
        }
    }

    public void exportiereAlleBuchungen(Path ziel) throws IOException {
        Map<Long, Konto> kontenById = new HashMap<>();
        for (Konto k : kontoRepository.findAll()) {
            kontenById.put(k.getId(), k);
        }
        Map<Long, Kategorie> kategorienById = new HashMap<>();
        for (Kategorie k : kategorieRepository.findAll()) {
            kategorienById.put(k.getId(), k);
        }
        Map<Long, Mitglied> mitgliederById = new HashMap<>();
        for (Mitglied m : mitgliedRepository.findAll()) {
            mitgliederById.put(m.getId(), m);
        }

        List<Buchung> buchungen = buchungRepository.findAll();
        Files.createDirectories(ziel.getParent() != null ? ziel.getParent() : ziel);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(ziel.toFile()), StandardCharsets.UTF_8)) {
            writer.write("Datum;Konto;Betrag;Kategorie;Mitglied;Verwendungszweck;Gegenkonto IBAN;"
                    + "Gegenkonto Name;Quelle;Storniert;Erstellt von;Erstellt am\n");
            for (Buchung b : buchungen) {
                Konto konto = kontenById.get(b.getKontoId());
                Kategorie kategorie = kategorienById.get(b.getKategorieId());
                Mitglied mitglied = mitgliederById.get(b.getMitgliedId());
                writer.write(csv(b.getBuchungsdatum().format(DATUM_FORMAT)) + ";"
                        + csv(konto != null ? konto.getBezeichnung() : "") + ";"
                        + csv(b.getBetragEuro().toPlainString()) + ";"
                        + csv(kategorie != null ? kategorie.getName() : "") + ";"
                        + csv(mitglied != null ? mitglied.getVollerName() : "") + ";"
                        + csv(b.getVerwendungszweck()) + ";"
                        + csv(b.getGegenkontoIban()) + ";"
                        + csv(b.getGegenkontoName()) + ";"
                        + csv(b.getQuelle().name()) + ";"
                        + csv(b.isStorniert() ? "Ja" : "Nein") + ";"
                        + csv(b.getErstelltVon()) + ";"
                        + csv(b.getErstelltAm() != null ? b.getErstelltAm().toString() : "") + "\n");
            }
        }
    }

    private String csv(String wert) {
        if (wert == null) {
            return "";
        }
        String bereinigt = wert.replace("\"", "\"\"");
        return bereinigt.contains(";") || bereinigt.contains("\"") ? "\"" + bereinigt + "\"" : bereinigt;
    }
}
