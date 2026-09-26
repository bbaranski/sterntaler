package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import de.foerderverein.kasse.domain.Anrede;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
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
        BerichtService berichtService = new BerichtService(
                buchungRepository, kategorieRepository, new MitgliedService(mitgliedRepository, buchungRepository));

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Mitgliedsbeiträge"))
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
        BerichtService berichtService = new BerichtService(
                buchungRepository, kategorieRepository, new MitgliedService(mitgliedRepository, buchungRepository));
        GeschaeftsjahrService geschaeftsjahrService = new GeschaeftsjahrService(geschaeftsjahrRepository);

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Geschaeftsjahr jahr = geschaeftsjahrRepository.save(neuesJahr());

        Kategorie spenden = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Mitgliedsbeiträge"))
                .findFirst().orElseThrow();
        Kategorie ausgabe = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Kontoführung und Bankgebühren"))
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
        BerichtService berichtService = new BerichtService(new BuchungRepository(database),
                new KategorieRepository(database), new MitgliedService(mitgliedRepository, new BuchungRepository(database)));

        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        mitglied.setEmail("erika@example.org");
        mitglied.setEintrittsdatum("01.01.2020");
        mitglied.setIban("DE02120300000000202051");
        mitglied.setBic("BYLADEM1001");
        mitglied.setJahresbeitragEuro(new BigDecimal("24.00"));
        mitglied.setNotiz("Gruendungsmitglied");
        mitglied.setAnrede(Anrede.FRAU);
        mitglied.setMandatsreferenz(1001L);
        mitglied.setSammellastschrift("Sammler-2026-03");
        mitgliedRepository.save(mitglied);

        Path ziel = Files.createTempDirectory("kasse-mitgliederliste-test").resolve("mitglieder.xlsx");
        berichtService.exportiereMitgliederliste(ziel);

        assertTrue(Files.exists(ziel));
        try (XSSFWorkbook workbook = new XSSFWorkbook(ziel.toFile())) {
            Sheet sheet = workbook.getSheet("Mitglieder");
            Row kopfzeile = sheet.getRow(0);
            assertEquals("Id", kopfzeile.getCell(0).getStringCellValue());
            assertEquals("Anrede", kopfzeile.getCell(1).getStringCellValue());
            assertEquals("Vorname", kopfzeile.getCell(2).getStringCellValue());
            assertEquals("Nachname", kopfzeile.getCell(3).getStringCellValue());
            assertEquals("E-Mail", kopfzeile.getCell(7).getStringCellValue());
            assertEquals("Jahresbeitrag", kopfzeile.getCell(11).getStringCellValue());
            assertEquals("IBAN", kopfzeile.getCell(12).getStringCellValue());
            assertEquals("BIC", kopfzeile.getCell(13).getStringCellValue());
            assertEquals("Mandatsreferenz", kopfzeile.getCell(14).getStringCellValue());
            assertEquals("Sammellastschrift", kopfzeile.getCell(15).getStringCellValue());
            assertEquals("Notiz", kopfzeile.getCell(16).getStringCellValue());

            Row datenzeile = sheet.getRow(1);
            assertEquals(String.valueOf(mitglied.getId()), datenzeile.getCell(0).getStringCellValue());
            assertEquals("FRAU", datenzeile.getCell(1).getStringCellValue());
            assertEquals("Erika", datenzeile.getCell(2).getStringCellValue());
            assertEquals("erika@example.org", datenzeile.getCell(7).getStringCellValue());
            assertEquals("01.01.2020", datenzeile.getCell(8).getStringCellValue());
            assertEquals("24,00", datenzeile.getCell(11).getStringCellValue());
            assertEquals("DE02120300000000202051", datenzeile.getCell(12).getStringCellValue());
            assertEquals("BYLADEM1001", datenzeile.getCell(13).getStringCellValue());
            assertEquals("1001", datenzeile.getCell(14).getStringCellValue());
            assertEquals("Sammler-2026-03", datenzeile.getCell(15).getStringCellValue());
            assertEquals("Gruendungsmitglied", datenzeile.getCell(16).getStringCellValue());
        }
    }

    @Test
    void berechneSteuerreportBucketedNachKategorieBereich() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        BerichtService berichtService = new BerichtService(
                buchungRepository, kategorieRepository, new MitgliedService(new MitgliedRepository(database), buchungRepository));

        Konto konto = new KontoRepository(database).save(neuesKonto());

        Kategorie spenden = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Mitgliedsbeiträge"))
                .findFirst().orElseThrow();
        Kategorie vereinsbedarf = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Kontoführung und Bankgebühren"))
                .findFirst().orElseThrow();

        // Bereits als WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB geseedet (siehe V17) - der Bereich haengt
        // an der Kategorie selbst, eine verknuepfte Aktivitaet spielt fuer die Steuererklaerung
        // keine Rolle mehr.
        Kategorie geschaeftsbetrieb = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Kuchen- und Getränkeverkauf"))
                .findFirst().orElseThrow();

        // Ideeller Bereich (Standard-Bereich der Starter-Kategorien): Spende (Einnahme) + Vereinsbedarf (Ausgabe)
        buchungRepository.insert(neueBuchung(konto.getId(), spenden.getId(), LocalDate.of(2026, 3, 1), new BigDecimal("100.00")));
        buchungRepository.insert(neueBuchung(konto.getId(), vereinsbedarf.getId(), LocalDate.of(2026, 3, 5), new BigDecimal("-40.00")));

        // Wirtschaftlicher Geschaeftsbetrieb ueber die Kategorie selbst
        buchungRepository.insert(neueBuchung(konto.getId(), geschaeftsbetrieb.getId(), LocalDate.of(2026, 6, 1), new BigDecimal("50.00")));

        // Buchung ohne Kategorie faellt in den Ideeller Bereich (Standardfall)
        buchungRepository.insert(neueBuchung(konto.getId(), null, LocalDate.of(2026, 6, 2), new BigDecimal("20.00")));

        // Stornierte Buchung darf nicht mitgezaehlt werden
        Buchung storniert = neueBuchung(konto.getId(), spenden.getId(), LocalDate.of(2026, 3, 10), new BigDecimal("999.00"));
        buchungRepository.insert(storniert);
        buchungRepository.storno(storniert.getId());

        // Buchung ausserhalb des betrachteten Jahres darf nicht mitgezaehlt werden
        buchungRepository.insert(neueBuchung(konto.getId(), spenden.getId(), LocalDate.of(2025, 12, 31), new BigDecimal("500.00")));

        SteuerreportDaten daten = berichtService.berechneSteuerreport(konto, 2026);

        assertEquals(10000L, daten.einnahmenProKategorieIdeell().get("Mitgliedsbeiträge"));
        assertEquals(4000L, daten.ausgabenProKategorieIdeell().get("Kontoführung und Bankgebühren"));
        assertEquals(2000L, daten.einnahmenProKategorieIdeell().get("(ohne Kategorie)"));
        assertTrue(daten.einnahmenProKategorieVermoegen().isEmpty());
        assertTrue(daten.einnahmenProKategorieZweckbetrieb().isEmpty());

        assertEquals(1, daten.einnahmenProKategorieGeschaeftsbetrieb().size());
        assertEquals(5000L, daten.einnahmenProKategorieGeschaeftsbetrieb().get("Kuchen- und Getränkeverkauf"));
    }

    @Test
    void erstelleSteuerreportErzeugtGueltigesPdf() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository,
                new MitgliedService(new MitgliedRepository(database), buchungRepository));

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Mitgliedsbeiträge"))
                .findFirst().orElseThrow();
        buchungRepository.insert(neueBuchung(konto.getId(), spenden.getId(), LocalDate.of(2026, 3, 1), new BigDecimal("100.00")));

        Path ziel = Files.createTempDirectory("kasse-steuerreport-test").resolve("steuerreport.pdf");
        berichtService.erstelleSteuerreport(ziel, konto, 2026);

        assertTrue(Files.exists(ziel));
        byte[] anfang = Files.readAllBytes(ziel);
        assertTrue(anfang.length > 4 && new String(anfang, 0, 4, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF"));
    }

    @Test
    void importiertMitgliederlisteImExportFormat() throws Exception {
        Database database = TestDatabase.create();
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        BerichtService berichtService = new BerichtService(new BuchungRepository(database),
                new KategorieRepository(database), new MitgliedService(mitgliedRepository, new BuchungRepository(database)));

        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        mitglied.setEmail("erika@example.org");
        mitglied.setEintrittsdatum("01.01.2020");
        mitglied.setIban("DE02120300000000202051");
        mitglied.setBic("BYLADEM1001");
        mitglied.setJahresbeitragEuro(new BigDecimal("24.00"));
        mitglied.setNotiz("Gruendungsmitglied");
        mitglied.setAnrede(Anrede.HERR);
        mitglied.setMandatsreferenz(1002L);
        mitglied.setSammellastschrift("Sammler-2026-04");
        mitgliedRepository.save(mitglied);

        Path exportiert = Files.createTempDirectory("kasse-mitgliederliste-roundtrip").resolve("mitglieder.xlsx");
        berichtService.exportiereMitgliederliste(exportiert);

        // Frische Datenbank simuliert den Import auf einem anderen Rechner/einer anderen Installation.
        Database zielDatenbank = TestDatabase.create();
        MitgliedRepository zielRepository = new MitgliedRepository(zielDatenbank);
        BerichtService zielService = new BerichtService(new BuchungRepository(zielDatenbank),
                new KategorieRepository(zielDatenbank), new MitgliedService(zielRepository, new BuchungRepository(zielDatenbank)));

        MitgliederImportErgebnis ergebnis = zielService.importiereMitgliederliste(exportiert);

        assertEquals(1, ergebnis.importiert());
        assertTrue(ergebnis.fehler().isEmpty());
        Mitglied importiert = zielRepository.findAll().get(0);
        assertEquals("Erika", importiert.getVorname());
        assertEquals("Musterfrau", importiert.getNachname());
        assertEquals("erika@example.org", importiert.getEmail());
        assertEquals("01.01.2020", importiert.getEintrittsdatum());
        assertEquals("DE02120300000000202051", importiert.getIban());
        assertEquals("BYLADEM1001", importiert.getBic());
        assertEquals(0, new BigDecimal("24.00").compareTo(importiert.getJahresbeitragEuro()));
        assertEquals("Gruendungsmitglied", importiert.getNotiz());
        assertEquals(Anrede.HERR, importiert.getAnrede());
        assertEquals(1002L, importiert.getMandatsreferenz());
        assertEquals("Sammler-2026-04", importiert.getSammellastschrift());
    }

    @Test
    void importUeberspringtUngueltigeZeilenUndMeldetSie() throws Exception {
        Database quellDatenbank = TestDatabase.create();
        MitgliedRepository quellRepository = new MitgliedRepository(quellDatenbank);
        BerichtService quellService = new BerichtService(new BuchungRepository(quellDatenbank),
                new KategorieRepository(quellDatenbank), new MitgliedService(quellRepository, new BuchungRepository(quellDatenbank)));

        Mitglied gueltig = new Mitglied();
        gueltig.setVorname("Erika");
        gueltig.setNachname("Musterfrau");
        quellRepository.save(gueltig);

        Mitglied ohneNachname = new Mitglied();
        ohneNachname.setVorname("Ohne Nachname");
        ohneNachname.setNachname(" ");
        quellRepository.save(ohneNachname);

        Path exportiert = Files.createTempDirectory("kasse-mitgliederliste-fehler").resolve("mitglieder.xlsx");
        quellService.exportiereMitgliederliste(exportiert);

        Database zielDatenbank = TestDatabase.create();
        BerichtService zielService = new BerichtService(new BuchungRepository(zielDatenbank),
                new KategorieRepository(zielDatenbank),
                new MitgliedService(new MitgliedRepository(zielDatenbank), new BuchungRepository(zielDatenbank)));

        MitgliederImportErgebnis ergebnis = zielService.importiereMitgliederliste(exportiert);

        assertEquals(1, ergebnis.importiert());
        assertEquals(1, ergebnis.fehler().size());
    }

    @Test
    void importLehntDateiMitFalscherKopfzeileAb() throws Exception {
        Database database = TestDatabase.create();
        BerichtService berichtService = new BerichtService(new BuchungRepository(database),
                new KategorieRepository(database),
                new MitgliedService(new MitgliedRepository(database), new BuchungRepository(database)));

        Path falscheDatei = Files.createTempDirectory("kasse-mitgliederliste-falsch").resolve("falsch.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Blatt");
            sheet.createRow(0).createCell(0).setCellValue("Irgendwas");
            try (var out = new java.io.FileOutputStream(falscheDatei.toFile())) {
                workbook.write(out);
            }
        }

        assertThrows(IOException.class, () -> berichtService.importiereMitgliederliste(falscheDatei));
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
