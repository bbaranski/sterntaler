package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.KategorieTyp;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class BerichtServiceTest {

    @Test
    void erstelltGueltigesPdf() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository, mitgliedRepository);

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Spenden") && k.getTyp() == KategorieTyp.EINNAHME)
                .findFirst().orElseThrow();

        Buchung buchung = new Buchung();
        buchung.setKontoId(konto.getId());
        buchung.setKategorieId(spenden.getId());
        buchung.setBuchungsdatum(LocalDate.of(2026, 3, 1));
        buchung.setBetragEuro(new BigDecimal("100.00"));
        buchung.setQuelle(BuchungsQuelle.MANUELL);
        buchungRepository.insert(buchung);

        Geschaeftsjahr jahr = new GeschaeftsjahrRepository(database).save(neuesJahr());

        Path ziel = Files.createTempDirectory("kasse-bericht-test").resolve("kassenbericht.pdf");
        berichtService.erstelleKassenbericht(ziel, konto, jahr);

        assertTrue(Files.exists(ziel));
        byte[] anfang = Files.readAllBytes(ziel);
        assertTrue(anfang.length > 4 && new String(anfang, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF"));
    }

    /** Stellt sicher, dass der im Kassenbericht berechnete Endsaldo mit dem letzten Eintrag von
     * GeschaeftsjahrService.berechneLaufendeSalden (Buchungsuebersicht) fuer dieselbe
     * (Konto, Geschaeftsjahr)-Kombination uebereinstimmt - beide Subsysteme duerfen nie
     * unterschiedliche Saldi anzeigen. Der PDF-Inhalt selbst wird nicht geparst (dafuer gibt es
     * keine Text-Extraktion in diesem Projekt), stattdessen wird der erwartete Endsaldo aus den
     * Test-Eingabedaten unabhaengig nachgerechnet und gegen berechneLaufendeSalden verglichen. */
    @Test
    void endsaldoIstKonsistentMitLaufendemSaldoDerBuchungsuebersicht() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        GeschaeftsjahrRepository geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository, mitgliedRepository);
        GeschaeftsjahrService geschaeftsjahrService = new GeschaeftsjahrService(geschaeftsjahrRepository);

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Geschaeftsjahr jahr = geschaeftsjahrRepository.save(neuesJahr());

        Kategorie spenden = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Spenden") && k.getTyp() == KategorieTyp.EINNAHME)
                .findFirst().orElseThrow();
        Kategorie ausgabe = kategorieRepository.findAll().stream()
                .filter(k -> k.getTyp() == KategorieTyp.AUSGABE)
                .findFirst().orElseThrow();

        Buchung einnahme = neueBuchung(konto.getId(), spenden.getId(), LocalDate.of(2026, 3, 1), new BigDecimal("100.00"));
        buchungRepository.insert(einnahme);
        Buchung ausgabeBuchung = neueBuchung(konto.getId(), ausgabe.getId(), LocalDate.of(2026, 3, 5), new BigDecimal("-30.00"));
        buchungRepository.insert(ausgabeBuchung);
        Buchung stornierteBuchung = neueBuchung(konto.getId(), spenden.getId(), LocalDate.of(2026, 3, 10), new BigDecimal("999.00"));
        buchungRepository.insert(stornierteBuchung);
        buchungRepository.storno(stornierteBuchung.getId());

        long erwarteterEndsaldo = jahr.getStartsaldoCent() + 10000 - 3000; // Startsaldo + Einnahme - Ausgabe, storniert zaehlt nicht

        Path ziel = Files.createTempDirectory("kasse-bericht-test").resolve("kassenbericht.pdf");
        berichtService.erstelleKassenbericht(ziel, konto, jahr);
        assertTrue(Files.exists(ziel));

        List<Buchung> alleBuchungen = buchungRepository.findAll();
        Map<Long, Long> laufendeSalden = geschaeftsjahrService.berechneLaufendeSalden(alleBuchungen, List.of(jahr));

        assertEquals(erwarteterEndsaldo, laufendeSalden.get(ausgabeBuchung.getId()));
    }

    @Test
    void erstelltMitgliederlisteAlsExcel() throws Exception {
        Database database = TestDatabase.create();
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        BerichtService berichtService = new BerichtService(
                new BuchungRepository(database), new KategorieRepository(database), mitgliedRepository);

        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        mitglied.setEmail("erika@example.org");
        mitglied.setEintrittsdatum(LocalDate.of(2020, 1, 1));
        mitgliedRepository.save(mitglied);

        Path ziel = Files.createTempDirectory("kasse-mitgliederliste-test").resolve("mitglieder.xlsx");
        berichtService.exportiereMitgliederliste(ziel);

        assertTrue(Files.exists(ziel));
        try (XSSFWorkbook workbook = new XSSFWorkbook(ziel.toFile())) {
            Sheet sheet = workbook.getSheet("Mitglieder");
            Row kopfzeile = sheet.getRow(0);
            assertEquals("Vorname", kopfzeile.getCell(0).getStringCellValue());
            assertEquals("E-Mail", kopfzeile.getCell(5).getStringCellValue());

            Row datenzeile = sheet.getRow(1);
            assertEquals("Erika", datenzeile.getCell(0).getStringCellValue());
            assertEquals("erika@example.org", datenzeile.getCell(5).getStringCellValue());
            assertEquals("01.01.2020", datenzeile.getCell(6).getStringCellValue());
        }
    }

    private Buchung neueBuchung(Long kontoId, Long kategorieId, LocalDate datum, BigDecimal betrag) {
        Buchung buchung = new Buchung();
        buchung.setKontoId(kontoId);
        buchung.setKategorieId(kategorieId);
        buchung.setBuchungsdatum(datum);
        buchung.setBetragEuro(betrag);
        buchung.setQuelle(BuchungsQuelle.MANUELL);
        return buchung;
    }

    private Konto neuesKonto() {
        Konto konto = new Konto();
        konto.setBezeichnung("Vereinskonto");
        konto.setIban("DE02120300000000202051");
        return konto;
    }

    private Geschaeftsjahr neuesJahr() {
        Geschaeftsjahr jahr = new Geschaeftsjahr();
        jahr.setBezeichnung("2026");
        jahr.setStartDatum(LocalDate.of(2026, 1, 1));
        jahr.setEndDatum(LocalDate.of(2026, 12, 31));
        jahr.setStartsaldoCent(50000);
        return jahr;
    }
}
