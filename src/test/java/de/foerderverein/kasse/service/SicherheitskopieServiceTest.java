package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Beleg;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.BelegRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;
import de.foerderverein.kasse.persistence.VereinRepository;

class SicherheitskopieServiceTest {

    private static final Pattern ORDNERNAME_MUSTER = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}-\\d{2}-\\d{2}");

    private SicherheitskopieService service;
    private Path belegeVerzeichnis;
    private Path databaseFile;
    private Beleg beleg;

    @BeforeEach
    void setUp() throws IOException {
        databaseFile = Files.createTempDirectory("kasse-test-db").resolve("test.db");
        Database database = new Database(databaseFile);
        database.migrate();
        belegeVerzeichnis = Files.createTempDirectory("kasse-test-belege");

        Konto konto = new KontoRepository(database).save(neuesKonto());
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Buchung buchung = neueBuchung(konto.getId());
        buchung.setRechnungsnummer("RE-2026-042");
        buchungRepository.insert(buchung);

        GeschaeftsjahrService geschaeftsjahrService = new GeschaeftsjahrService(new GeschaeftsjahrRepository(database));
        BelegService belegService = new BelegService(new BelegRepository(database), geschaeftsjahrService,
                belegeVerzeichnis);
        Path hochzuladendeDatei = Files.createTempFile("beleg-test", ".pdf");
        Files.write(hochzuladendeDatei, new byte[] {1, 2, 3});
        beleg = belegService.hochladen(buchung, hochzuladendeDatei.toFile());

        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        new MitgliedRepository(database).save(mitglied);

        MitgliedService mitgliedService = new MitgliedService(new MitgliedRepository(database),
                new AuditLogRepository(database));
        BerichtService berichtService = new BerichtService(buchungRepository, new KategorieRepository(database),
                new AktivitaetRepository(database), mitgliedService,
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));
        KontobewegungenExportService kontobewegungenExportService = new KontobewegungenExportService(
                buchungRepository, new KontoRepository(database), new KategorieRepository(database),
                new AktivitaetRepository(database));

        service = new SicherheitskopieService(kontobewegungenExportService, berichtService, belegeVerzeichnis,
                databaseFile);
    }

    @Test
    void erstelltVollstaendigeSicherheitskopie() throws IOException {
        Path zielOrdner = Files.createTempDirectory("kasse-sicherheitskopie-test");

        Path erstellt = service.erstelleSicherheitskopie(zielOrdner);

        assertTrue(Files.isDirectory(erstellt));
        assertTrue(ORDNERNAME_MUSTER.matcher(erstellt.getFileName().toString()).matches());

        assertTrue(Files.exists(erstellt.resolve("Kontobewegungen.xlsx")));
        assertTrue(Files.exists(erstellt.resolve("Mitgliederliste.xlsx")));

        Path kopierteDatenbank = erstellt.resolve(databaseFile.getFileName());
        assertTrue(Files.exists(kopierteDatenbank));
        assertArrayEquals(Files.readAllBytes(databaseFile), Files.readAllBytes(kopierteDatenbank));

        Path kopierterBeleg = erstellt.resolve("Belege").resolve(beleg.getSpeicherpfad());
        assertTrue(Files.exists(kopierterBeleg));
        assertArrayEquals(Files.readAllBytes(belegeVerzeichnis.resolve(beleg.getSpeicherpfad())),
                Files.readAllBytes(kopierterBeleg));
    }

    private Buchung neueBuchung(Long kontoId) {
        Buchung buchung = new Buchung();
        buchung.setKontoId(kontoId);
        buchung.setBuchungsdatum(LocalDate.of(2026, 1, 15));
        buchung.setBetragEuro(new BigDecimal("42.50"));
        buchung.setVerwendungszweck("Testbuchung");
        buchung.setQuelle(BuchungsQuelle.MANUELL);
        return buchung;
    }

    private Konto neuesKonto() {
        Konto konto = new Konto();
        konto.setBezeichnung("Vereinskonto");
        konto.setIban("DE02120300000000202051");
        return konto;
    }
}
