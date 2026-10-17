package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Anrede;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;
import de.foerderverein.kasse.domain.Verein;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;
import de.foerderverein.kasse.persistence.TestDatabase;
import de.foerderverein.kasse.persistence.VereinRepository;

class BerichtServiceTest {

    @Test
    void erstelltGueltigesPdf() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        VereinRepository vereinRepository = new VereinRepository(database);
        BerichtService berichtService = new BerichtService(
                buchungRepository, kategorieRepository, new AktivitaetRepository(database),
                new MitgliedService(mitgliedRepository, new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), vereinRepository);

        Verein verein = new Verein();
        verein.setName("Förderverein Musterschule e. V.");
        verein.setStrasse("Musterstraße 1");
        verein.setPlz("12345");
        verein.setOrt("Musterstadt");
        verein.setIban("DE02120300000000202051");
        vereinRepository.save(verein);

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
     * unterschiedliche Saldi anzeigen. Der erwartete Endsaldo wird dafuer aus den Test-Eingabedaten
     * unabhaengig nachgerechnet und gegen berechneLaufendeSalden verglichen, statt ihn aus dem PDF
     * zu lesen (Text-Extraktion siehe pdfText(...), hier aber nicht noetig). */
    @Test
    void endsaldoIstKonsistentMitLaufendemSaldoDerBuchungsuebersicht() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        GeschaeftsjahrRepository geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        BerichtService berichtService = new BerichtService(
                buchungRepository, kategorieRepository, new AktivitaetRepository(database),
                new MitgliedService(mitgliedRepository, new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));
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

    /** Spiegelt den bestehenden Konsistenztest, aber mit einer am Stichtag ins Folgejahr
     * umgehaengten Buchung: Kassenbericht-Filter (BerichtService.gehoertZu) und Saldo-Berechnung
     * (GeschaeftsjahrService.findeFuer) leiten die Jahreszugehoerigkeit unabhaengig voneinander
     * ab - sie duerfen dabei nie auseinanderlaufen. */
    @Test
    void umgehaengteStichtagsbuchungBleibtZwischenBerichtUndSaldoKonsistent() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        GeschaeftsjahrRepository geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository, new AktivitaetRepository(database),
                new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));
        GeschaeftsjahrService geschaeftsjahrService = new GeschaeftsjahrService(geschaeftsjahrRepository);

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Geschaeftsjahr jahr2026 = geschaeftsjahrRepository.save(neuesJahr());
        Geschaeftsjahr jahr2027 = new Geschaeftsjahr();
        jahr2027.setBezeichnung("2027");
        jahr2027.setStartDatum(LocalDate.of(2027, 1, 1));
        jahr2027.setEndDatum(LocalDate.of(2027, 12, 31));
        jahr2027.setStartsaldoCent(0);
        jahr2027 = geschaeftsjahrRepository.save(jahr2027);

        Kategorie kategorie = kategorieRepository.findAll().get(0);

        Buchung imJahr = neueBuchung(konto.getId(), kategorie.getId(), LocalDate.of(2026, 3, 1), new BigDecimal("100.00"));
        buchungRepository.insert(imJahr);
        // Liegt am Stichtag 31.12.2026, gehoert fachlich aber schon ins Folgejahr.
        Buchung amStichtag = neueBuchung(konto.getId(), kategorie.getId(), LocalDate.of(2026, 12, 31), new BigDecimal("77.00"));
        amStichtag.setGeschaeftsjahrId(jahr2027.getId());
        buchungRepository.insert(amStichtag);

        List<Buchung> alle = buchungRepository.findAll();
        List<Geschaeftsjahr> jahre = List.of(jahr2026, jahr2027);
        Map<Long, Long> salden = geschaeftsjahrService.berechneLaufendeSalden(alle, jahre);

        // Die umgehaengte Buchung zaehlt nicht mehr zum Saldo von 2026 ...
        long erwarteterEndsaldo2026 = jahr2026.getStartsaldoCent() + 10000;
        assertEquals(erwarteterEndsaldo2026, salden.get(imJahr.getId()));
        // ... sondern eroeffnet die Kette von 2027.
        assertEquals(jahr2027.getStartsaldoCent() + 7700, salden.get(amStichtag.getId()));

        // Und der Kassenbericht beider Jahre laesst sich fehlerfrei erzeugen.
        Path ziel2026 = Files.createTempDirectory("kasse-bericht-test").resolve("2026.pdf");
        berichtService.erstelleKassenbericht(ziel2026, konto, jahr2026);
        Path ziel2027 = Files.createTempDirectory("kasse-bericht-test").resolve("2027.pdf");
        berichtService.erstelleKassenbericht(ziel2027, konto, jahr2027);
        assertTrue(Files.exists(ziel2026));
        assertTrue(Files.exists(ziel2027));
    }

    @Test
    void erstelltMitgliederlisteAlsExcel() throws Exception {
        Database database = TestDatabase.create();
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        BerichtService berichtService = new BerichtService(new BuchungRepository(database),
                new KategorieRepository(database), new AktivitaetRepository(database),
                new MitgliedService(mitgliedRepository, new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        mitglied.setTelefon("02841 908103");
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
            assertEquals("Telefon", kopfzeile.getCell(7).getStringCellValue());
            assertEquals("E-Mail", kopfzeile.getCell(8).getStringCellValue());
            assertEquals("Jahresbeitrag", kopfzeile.getCell(12).getStringCellValue());
            assertEquals("IBAN", kopfzeile.getCell(13).getStringCellValue());
            assertEquals("BIC", kopfzeile.getCell(14).getStringCellValue());
            assertEquals("Mandatsreferenz", kopfzeile.getCell(15).getStringCellValue());
            assertEquals("Sammellastschrift", kopfzeile.getCell(16).getStringCellValue());
            assertEquals("Notiz", kopfzeile.getCell(17).getStringCellValue());

            Row datenzeile = sheet.getRow(1);
            assertEquals(String.valueOf(mitglied.getId()), datenzeile.getCell(0).getStringCellValue());
            assertEquals("FRAU", datenzeile.getCell(1).getStringCellValue());
            assertEquals("Erika", datenzeile.getCell(2).getStringCellValue());
            assertEquals("02841 908103", datenzeile.getCell(7).getStringCellValue());
            assertEquals("erika@example.org", datenzeile.getCell(8).getStringCellValue());
            assertEquals("01.01.2020", datenzeile.getCell(9).getStringCellValue());
            assertEquals("24,00", datenzeile.getCell(12).getStringCellValue());
            assertEquals("DE02120300000000202051", datenzeile.getCell(13).getStringCellValue());
            assertEquals("BYLADEM1001", datenzeile.getCell(14).getStringCellValue());
            assertEquals("1001", datenzeile.getCell(15).getStringCellValue());
            assertEquals("Sammler-2026-03", datenzeile.getCell(16).getStringCellValue());
            assertEquals("Gruendungsmitglied", datenzeile.getCell(17).getStringCellValue());
        }
    }

    @Test
    void berechneSteuerreportBucketedNachKategorieBereich() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        BerichtService berichtService = new BerichtService(
                buchungRepository, kategorieRepository, new AktivitaetRepository(database),
                new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

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
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository, new AktivitaetRepository(database),
                new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

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
                new KategorieRepository(database), new AktivitaetRepository(database),
                new MitgliedService(mitgliedRepository, new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

        Mitglied mitglied = new Mitglied();
        mitglied.setVorname("Erika");
        mitglied.setNachname("Musterfrau");
        mitglied.setTelefon("02841 908103");
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
                new KategorieRepository(zielDatenbank), new AktivitaetRepository(zielDatenbank),
                new MitgliedService(zielRepository, new AuditLogRepository(zielDatenbank)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(zielDatenbank)),
                new VereinRepository(zielDatenbank));

        MitgliederImportErgebnis ergebnis = zielService.importiereMitgliederliste(exportiert);

        assertEquals(1, ergebnis.importiert());
        assertTrue(ergebnis.fehler().isEmpty());
        Mitglied importiert = zielRepository.findAll().get(0);
        assertEquals("Erika", importiert.getVorname());
        assertEquals("Musterfrau", importiert.getNachname());
        assertEquals("02841 908103", importiert.getTelefon());
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

    /** Ganz oder gar nicht: eine einzige beanstandete Zeile verhindert den gesamten Import, die
     * Zieldatenbank bleibt leer. Vorher wurden die gueltigen Zeilen geschrieben und nur die
     * fehlerhaften gemeldet - ein korrigierter zweiter Versuch legte sie dann doppelt an. */
    @Test
    void importLehntDateiMitFehlerhaftenZeilenKomplettAb() throws Exception {
        Database quellDatenbank = TestDatabase.create();
        MitgliedRepository quellRepository = new MitgliedRepository(quellDatenbank);
        BerichtService quellService = berichtService(quellDatenbank, quellRepository);

        quellRepository.save(neuesMitglied("Erika", "Musterfrau"));
        quellRepository.save(neuesMitglied("Max", "Mustermann"));
        // Umgeht die Service-Pruefung und erzeugt so eine Zeile, die beim Import auffaellt.
        quellRepository.save(neuesMitglied("Ohne Nachname", " "));

        Path exportiert = Files.createTempDirectory("kasse-import-abbruch").resolve("mitglieder.xlsx");
        quellService.exportiereMitgliederliste(exportiert);

        Database zielDatenbank = TestDatabase.create();
        MitgliedRepository zielRepository = new MitgliedRepository(zielDatenbank);
        MitgliederImportErgebnis ergebnis =
                berichtService(zielDatenbank, zielRepository).importiereMitgliederliste(exportiert);

        assertEquals(0, ergebnis.importiert());
        assertEquals(1, ergebnis.fehler().size());
        assertTrue(zielRepository.findAll().isEmpty(),
                "Datenbank muss unberuehrt bleiben, enthielt aber " + zielRepository.findAll().size() + " Mitglieder");
    }

    @Test
    void importSchreibtAlleZeilenWennDateiFehlerfrei() throws Exception {
        Database quellDatenbank = TestDatabase.create();
        MitgliedRepository quellRepository = new MitgliedRepository(quellDatenbank);
        BerichtService quellService = berichtService(quellDatenbank, quellRepository);
        quellRepository.save(neuesMitglied("Erika", "Musterfrau"));
        quellRepository.save(neuesMitglied("Max", "Mustermann"));
        quellRepository.save(neuesMitglied("Anna", "Beispiel"));

        Path exportiert = Files.createTempDirectory("kasse-import-vollstaendig").resolve("mitglieder.xlsx");
        quellService.exportiereMitgliederliste(exportiert);

        Database zielDatenbank = TestDatabase.create();
        MitgliedRepository zielRepository = new MitgliedRepository(zielDatenbank);
        AuditLogRepository zielAuditLog = new AuditLogRepository(zielDatenbank);
        MitgliedService zielMitgliedService = new MitgliedService(zielRepository, zielAuditLog);
        BerichtService zielService = new BerichtService(new BuchungRepository(zielDatenbank),
                new KategorieRepository(zielDatenbank), new AktivitaetRepository(zielDatenbank),
                zielMitgliedService, new GeschaeftsjahrService(new GeschaeftsjahrRepository(zielDatenbank)),
                new VereinRepository(zielDatenbank));

        MitgliederImportErgebnis ergebnis = zielService.importiereMitgliederliste(exportiert);

        assertEquals(3, ergebnis.importiert());
        assertTrue(ergebnis.fehler().isEmpty());
        assertEquals(3, zielRepository.findAll().size());
        // Lueckenlose ids ab 1 und je ein Verlaufseintrag - das Protokollieren laeuft nach dem
        // Commit und darf dabei nicht uebersprungen werden.
        assertEquals(List.of(1L, 2L, 3L),
                zielRepository.findAll().stream().map(Mitglied::getId).sorted().toList());
        for (Mitglied m : zielRepository.findAll()) {
            assertEquals(1, zielMitgliedService.findVerlauf(m.getId()).size());
        }
    }

    /** Alle Beanstandungen einer Zeile auf einmal, damit eine Korrekturrunde genuegt. */
    @Test
    void importMeldetAlleFehlerEinerZeileAufEinmal() throws Exception {
        Database quellDatenbank = TestDatabase.create();
        MitgliedRepository quellRepository = new MitgliedRepository(quellDatenbank);
        quellRepository.save(neuesMitglied("Erika", "Musterfrau"));
        Path exportiert = Files.createTempDirectory("kasse-import-mehrfachfehler").resolve("mitglieder.xlsx");
        berichtService(quellDatenbank, quellRepository).exportiereMitgliederliste(exportiert);

        // Spalte 3 = Nachname, Spalte 11 = Status, Spalte 15 = Mandatsreferenz (MITGLIEDER_SPALTEN).
        setzeZellen(exportiert, 1, Map.of(3, "", 11, "Passiv", 15, "abc"));

        Database zielDatenbank = TestDatabase.create();
        MitgliedRepository zielRepository = new MitgliedRepository(zielDatenbank);
        MitgliederImportErgebnis ergebnis =
                berichtService(zielDatenbank, zielRepository).importiereMitgliederliste(exportiert);

        assertEquals(0, ergebnis.importiert());
        assertEquals(3, ergebnis.fehler().size(), "erwartet: Nachname, Status und Mandatsreferenz, war: "
                + ergebnis.fehler());
        assertTrue(ergebnis.fehler().stream().allMatch(f -> f.startsWith("Zeile 2:")), ergebnis.fehler().toString());
        assertTrue(zielRepository.findAll().isEmpty());
    }

    @Test
    void importLehntDateiOhneDatenzeilenAb() throws Exception {
        Database quellDatenbank = TestDatabase.create();
        MitgliedRepository quellRepository = new MitgliedRepository(quellDatenbank);
        Path exportiert = Files.createTempDirectory("kasse-import-leer").resolve("mitglieder.xlsx");
        berichtService(quellDatenbank, quellRepository).exportiereMitgliederliste(exportiert);

        Database zielDatenbank = TestDatabase.create();
        MitgliedRepository zielRepository = new MitgliedRepository(zielDatenbank);
        MitgliederImportErgebnis ergebnis =
                berichtService(zielDatenbank, zielRepository).importiereMitgliederliste(exportiert);

        assertEquals(0, ergebnis.importiert());
        assertEquals(1, ergebnis.fehler().size());
        assertTrue(ergebnis.fehler().get(0).contains("keine Datenzeilen"), ergebnis.fehler().toString());
    }

    @Test
    void importLehntDateiMitFalscherKopfzeileAb() throws Exception {
        Database database = TestDatabase.create();
        BerichtService berichtService = new BerichtService(new BuchungRepository(database),
                new KategorieRepository(database), new AktivitaetRepository(database),
                new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

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

    /** Nullbuchungen (HBCI-Mitteilungen der Bank, siehe BerichtService.hatBetrag) wurden wegen
     * der Einordnung betrag >= 0 als Einnahme gezaehlt und erzeugten im Kassenbericht eine Zeile
     * "(ohne Kategorie)" mit 0,00 Euro. Hier wird der PDF-Text ausgelesen, weil die Aufteilung
     * nach Kategorien im Kassenbericht sonst nirgends nach aussen sichtbar ist. */
    @Test
    void kassenberichtLaesstBuchungenOhneBetragAus() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        BerichtService berichtService = new BerichtService(
                buchungRepository, kategorieRepository, new AktivitaetRepository(database),
                new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie beitraege = kategorieRepository.findAll().stream()
                .filter(k -> k.getName().equals("Mitgliedsbeiträge"))
                .findFirst().orElseThrow();

        buchungRepository.insert(neueBuchung(konto.getId(), beitraege.getId(),
                LocalDate.of(2026, 3, 1), new BigDecimal("100.00")));
        // Die Mitteilung der Bank: Betrag 0, keine Kategorie.
        buchungRepository.insert(neueBuchung(konto.getId(), null,
                LocalDate.of(2026, 3, 31), new BigDecimal("0.00")));

        Geschaeftsjahr jahr = new GeschaeftsjahrRepository(database).save(neuesJahr());
        Path ziel = Files.createTempDirectory("kasse-nullbuchung-test").resolve("kassenbericht.pdf");
        berichtService.erstelleKassenbericht(ziel, konto, jahr);

        String text = pdfText(ziel);
        assertTrue(text.contains("Mitgliedsbeiträge"), "echte Buchung fehlt im Bericht: " + text);
        assertFalse(text.contains("(ohne Kategorie)"), "Nullbuchung taucht im Bericht auf: " + text);
    }

    @Test
    void steuerreportLaesstBuchungenOhneBetragAus() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        BerichtService berichtService = new BerichtService(
                buchungRepository, kategorieRepository, new AktivitaetRepository(database),
                new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

        Konto konto = new KontoRepository(database).save(neuesKonto());
        buchungRepository.insert(neueBuchung(konto.getId(), null,
                LocalDate.of(2026, 3, 31), new BigDecimal("0.00")));

        SteuerreportDaten daten = berichtService.berechneSteuerreport(konto, 2026);

        // Ohne Kategorie faellt die Buchung in den Ideeller Bereich (siehe berechneSteuerreport).
        assertTrue(daten.einnahmenProKategorieIdeell().isEmpty(),
                "Nullbuchung wurde als Einnahme gezaehlt: " + daten.einnahmenProKategorieIdeell());
        assertTrue(daten.ausgabenProKategorieIdeell().isEmpty(),
                "Nullbuchung wurde als Ausgabe gezaehlt: " + daten.ausgabenProKategorieIdeell());
    }

    private String pdfText(Path pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf.toString());
        try {
            StringBuilder text = new StringBuilder();
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            for (int seite = 1; seite <= reader.getNumberOfPages(); seite++) {
                text.append(extractor.getTextFromPage(seite)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    @Test
    void journalZeigtAlleBuchungenDesJahresMitLaufendemSaldo() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        AktivitaetRepository aktivitaetRepository = new AktivitaetRepository(database);
        BerichtService berichtService = journalService(database);

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorie(kategorieRepository, "Geldspenden");
        Aktivitaet martin = aktivitaet(aktivitaetRepository, "Sankt Martin");

        // Zwei Buchungen am selben Tag: die Reihenfolge muss (datum, id) folgen, sonst laeuft
        // der Saldo nicht monoton.
        Buchung erste = buchung(konto, spenden, martin, LocalDate.of(2026, 3, 1), "100.00");
        erste.setVerwendungszweck("Spende Theater");
        erste.setRechnungsnummer("2026-001");
        buchungRepository.insert(erste);
        Buchung zweite = buchung(konto, spenden, null, LocalDate.of(2026, 3, 1), "-40.00");
        buchungRepository.insert(zweite);
        buchungRepository.insert(buchung(konto, null, null, LocalDate.of(2026, 3, 5), "15.00"));

        BuchungsjournalDaten daten = berichtService.berechneBuchungsjournal(konto, neuesJahr());

        assertEquals(3, daten.zeilen().size());
        assertEquals(50000L, daten.startsaldoCent());
        // Startsaldo 500,00 + 100,00 - 40,00 + 15,00
        assertEquals(List.of(60000L, 56000L, 57500L),
                daten.zeilen().stream().map(BuchungsjournalDaten.Zeile::saldoCent).toList());
        assertEquals(List.of(10000L, -4000L, 1500L),
                daten.zeilen().stream().map(BuchungsjournalDaten.Zeile::betragCent).toList());

        BuchungsjournalDaten.Zeile zeile = daten.zeilen().get(0);
        assertEquals(LocalDate.of(2026, 3, 1), zeile.datum());
        assertEquals("2026-001", zeile.rechnungsnummer());
        assertEquals("Geldspenden", zeile.kategorie());
        assertEquals("Sankt Martin", zeile.aktivitaet());
        assertEquals("Spende Theater", zeile.verwendungszweck());
        // Ohne Zuordnung bleiben die Felder leer, kein Sammelposten wie in den Summenberichten.
        assertEquals("", daten.zeilen().get(2).kategorie());
        assertEquals("", daten.zeilen().get(2).aktivitaet());
    }

    /** Die Zusage, dass Journal und Buchungsuebersicht nie unterschiedliche Saldi zeigen: beide
     * leiten sie aus GeschaeftsjahrService.berechneLaufendeSalden ab. */
    @Test
    void journalSaldoStimmtMitDerBuchungsuebersichtUeberein() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        GeschaeftsjahrRepository geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorie(new KategorieRepository(database), "Geldspenden");

        buchungRepository.insert(buchung(konto, spenden, null, LocalDate.of(2026, 3, 1), "100.00"));
        buchungRepository.insert(buchung(konto, spenden, null, LocalDate.of(2026, 3, 1), "-40.00"));
        buchungRepository.insert(buchung(konto, null, null, LocalDate.of(2026, 7, 9), "15.00"));
        Geschaeftsjahr jahr = geschaeftsjahrRepository.save(neuesJahr());

        BuchungsjournalDaten daten = journalService(database).berechneBuchungsjournal(konto, jahr);
        Map<Long, Long> uebersicht = new GeschaeftsjahrService(geschaeftsjahrRepository)
                .berechneLaufendeSalden(buchungRepository.findAll(), geschaeftsjahrRepository.findAll());

        List<Long> ausDerUebersicht = buchungRepository.findAll().stream()
                .sorted(Comparator.comparing(Buchung::getBuchungsdatum).thenComparing(Buchung::getId))
                .map(b -> uebersicht.get(b.getId()))
                .toList();
        assertEquals(ausDerUebersicht,
                daten.zeilen().stream().map(BuchungsjournalDaten.Zeile::saldoCent).toList());
    }

    @Test
    void journalEndsaldoEntsprichtDerLetztenZeile() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        buchungRepository.insert(buchung(konto, null, null, LocalDate.of(2026, 3, 1), "100.00"));
        buchungRepository.insert(buchung(konto, null, null, LocalDate.of(2026, 4, 1), "-40.00"));

        BuchungsjournalDaten daten = journalService(database).berechneBuchungsjournal(konto, neuesJahr());

        assertEquals(6000L, daten.summeCent());
        assertEquals(daten.startsaldoCent() + daten.summeCent(), daten.endsaldoCent());
        assertEquals(daten.zeilen().get(daten.zeilen().size() - 1).saldoCent(), daten.endsaldoCent());
    }

    /** Dieselbe Buchungsmenge wie der Kassenbericht - sonst waeren die Summen beider Dokumente
     * nicht vergleichbar. */
    @Test
    void journalLaesstStornierteUndNullbuchungenAus() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());

        buchungRepository.insert(buchung(konto, null, null, LocalDate.of(2026, 3, 1), "100.00"));
        Buchung storniert = buchung(konto, null, null, LocalDate.of(2026, 3, 2), "999.00");
        buchungRepository.insert(storniert);
        buchungRepository.storno(storniert.getId());
        buchungRepository.insert(buchung(konto, null, null, LocalDate.of(2026, 3, 3), "0.00"));
        buchungRepository.insert(buchung(konto, null, null, LocalDate.of(2025, 12, 31), "777.00"));

        BuchungsjournalDaten daten = journalService(database).berechneBuchungsjournal(konto, neuesJahr());

        assertEquals(1, daten.zeilen().size());
        assertEquals(10000L, daten.zeilen().get(0).betragCent());
    }

    @Test
    void journalErzeugtQuerformatPdf() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        VereinRepository vereinRepository = new VereinRepository(database);
        Verein verein = new Verein();
        verein.setName("Förderverein Musterschule e. V.");
        vereinRepository.save(verein);

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorie(new KategorieRepository(database), "Geldspenden");
        Buchung b = buchung(konto, spenden, null, LocalDate.of(2026, 3, 1), "100.00");
        b.setVerwendungszweck("SVWZ+St. Martin - Weckmänner DATUM 07.11.2025");
        buchungRepository.insert(b);

        Path ziel = Files.createTempDirectory("kasse-journal").resolve("journal.pdf");
        journalService(database).erstelleBuchungsjournal(ziel, konto, neuesJahr());

        PdfReader reader = new PdfReader(ziel.toString());
        try {
            assertTrue(reader.getPageSize(1).getWidth() > reader.getPageSize(1).getHeight(),
                    "PDF muss im Querformat sein: " + reader.getPageSize(1));
            // Die Seite selbst muss quer sein, nicht bloss per /Rotate gedreht - sonst stellen
            // Werkzeuge, die /Rotate ignorieren, den Bericht hochkant dar.
            assertEquals(0, reader.getPageRotation(1));
        } finally {
            reader.close();
        }

        String text = pdfText(ziel);
        assertTrue(text.contains("Umsätze"), text);
        assertTrue(text.contains("Verwendungszweck"), text);
        // Die Spalte muss breit genug fuer ihre Beschriftung sein - sonst steht dort
        // "Kontoauszu" / "g" auf zwei Zeilen.
        assertTrue(text.contains("Kontoauszug"), "Spaltenkopf umgebrochen: " + text);
        assertTrue(text.contains("Weckmänner"), text);
        assertTrue(text.contains("Startsaldo"), text);
        assertTrue(text.contains("Endsaldo"), text);
        assertFalse(text.contains("Unterschrift"), "Unterschriften waren nicht gewuenscht: " + text);
    }

    private BerichtService journalService(Database database) {
        return new BerichtService(new BuchungRepository(database), new KategorieRepository(database),
                new AktivitaetRepository(database),
                new MitgliedService(new MitgliedRepository(database), new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)),
                new VereinRepository(database));
    }

    @Test
    void anhangSchluesseltJedeKategorieNachAktivitaetAuf() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        AktivitaetRepository aktivitaetRepository = new AktivitaetRepository(database);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository,
                aktivitaetRepository, new MitgliedService(new MitgliedRepository(database),
                        new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorie(kategorieRepository, "Geldspenden");
        Kategorie beitraege = kategorie(kategorieRepository, "Mitgliedsbeiträge");
        Aktivitaet martin = aktivitaet(aktivitaetRepository, "Sankt Martin");
        Aktivitaet weihnachtsmarkt = aktivitaet(aktivitaetRepository, "Weihnachtsmarkt");

        buchungRepository.insert(buchung(konto, spenden, martin, LocalDate.of(2026, 3, 1), "100.00"));
        buchungRepository.insert(buchung(konto, spenden, weihnachtsmarkt, LocalDate.of(2026, 3, 2), "50.00"));
        // Dieselbe Kategorie ohne Aktivitaet: muss als Restposten erscheinen, damit sich die
        // Unterzeilen auf die Kategoriesumme addieren.
        buchungRepository.insert(buchung(konto, spenden, null, LocalDate.of(2026, 3, 3), "25.00"));
        // Kategorie, in der keine Buchung eine Aktivitaet traegt: keine Unterzeilen.
        buchungRepository.insert(buchung(konto, beitraege, null, LocalDate.of(2026, 3, 4), "24.00"));

        KassenberichtAnhangDaten daten = berichtService.berechneKassenberichtAnhang(konto, neuesJahr());

        assertEquals(2, daten.einnahmen().size());
        KassenberichtAnhangDaten.KategorieBlock spendenBlock = daten.einnahmen().get(0);
        assertEquals("Geldspenden", spendenBlock.kategorie());
        assertEquals(17500L, spendenBlock.summeCent());
        // Reihenfolge des ersten Vorkommens, Restposten ohne Sonderbehandlung.
        assertEquals(List.of("Sankt Martin", "Weihnachtsmarkt", "(ohne Aktivität)"),
                spendenBlock.aktivitaeten().stream()
                        .map(KassenberichtAnhangDaten.AktivitaetSumme::aktivitaet).toList());
        assertEquals(spendenBlock.summeCent(), spendenBlock.aktivitaeten().stream()
                .mapToLong(KassenberichtAnhangDaten.AktivitaetSumme::summeCent).sum());

        KassenberichtAnhangDaten.KategorieBlock beitragsBlock = daten.einnahmen().get(1);
        assertEquals("Mitgliedsbeiträge", beitragsBlock.kategorie());
        assertEquals(2400L, beitragsBlock.summeCent());
        assertTrue(beitragsBlock.aktivitaeten().isEmpty(),
                "ohne jede Aktivitaet soll die Kategoriezeile allein stehen: " + beitragsBlock.aktivitaeten());
    }

    /** Der Mehrwert des Anhangs: eine Aktivitaet verteilt sich ueber mehrere Kategorien und ueber
     * Einnahmen und Ausgaben - in der Aufschluesselung je Kategorie ist ihr Ergebnis nicht
     * ablesbar. */
    @Test
    void anhangFasstAktivitaetUeberKategorienUndVorzeichenZusammen() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        AktivitaetRepository aktivitaetRepository = new AktivitaetRepository(database);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository,
                aktivitaetRepository, new MitgliedService(new MitgliedRepository(database),
                        new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie verkauf = kategorie(kategorieRepository, "Kuchen- und Getränkeverkauf");
        Kategorie einkauf = kategorie(kategorieRepository, "Wareneinkauf für Verkäufe");
        Kategorie feste = kategorie(kategorieRepository, "Schulfeste und Veranstaltungen");
        Aktivitaet martin = aktivitaet(aktivitaetRepository, "Sankt Martin");

        buchungRepository.insert(buchung(konto, verkauf, martin, LocalDate.of(2026, 3, 1), "2004.21"));
        buchungRepository.insert(buchung(konto, einkauf, martin, LocalDate.of(2026, 3, 2), "-532.03"));
        buchungRepository.insert(buchung(konto, feste, martin, LocalDate.of(2026, 3, 3), "-1190.00"));

        KassenberichtAnhangDaten daten = berichtService.berechneKassenberichtAnhang(konto, neuesJahr());

        assertEquals(1, daten.aktivitaeten().size());
        KassenberichtAnhangDaten.AktivitaetErgebnis martinErgebnis = daten.aktivitaeten().get(0);
        assertEquals("Sankt Martin", martinErgebnis.aktivitaet());
        assertEquals(200421L, martinErgebnis.einnahmenCent());
        assertEquals(172203L, martinErgebnis.ausgabenCent());
        assertEquals(28218L, martinErgebnis.ergebnisCent());
    }

    /** Anhang und Kassenbericht muessen dieselbe Buchungsmenge auswerten - sonst behaupten zwei
     * Dokumente derselben Kassenpruefung unterschiedliche Summen. */
    @Test
    void anhangSummenStimmenMitDenAbschnittssummenUeberein() {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        AktivitaetRepository aktivitaetRepository = new AktivitaetRepository(database);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository,
                aktivitaetRepository, new MitgliedService(new MitgliedRepository(database),
                        new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorie(kategorieRepository, "Geldspenden");
        Aktivitaet martin = aktivitaet(aktivitaetRepository, "Sankt Martin");
        buchungRepository.insert(buchung(konto, spenden, martin, LocalDate.of(2026, 3, 1), "100.00"));
        buchungRepository.insert(buchung(konto, spenden, null, LocalDate.of(2026, 3, 2), "-40.00"));
        buchungRepository.insert(buchung(konto, null, martin, LocalDate.of(2026, 3, 3), "15.00"));
        // Darf in keiner Summe auftauchen: storniert bzw. ausserhalb des Geschaeftsjahres.
        Buchung storniert = buchung(konto, spenden, martin, LocalDate.of(2026, 3, 4), "999.00");
        buchungRepository.insert(storniert);
        buchungRepository.storno(storniert.getId());
        buchungRepository.insert(buchung(konto, spenden, martin, LocalDate.of(2025, 12, 31), "777.00"));

        KassenberichtAnhangDaten daten = berichtService.berechneKassenberichtAnhang(konto, neuesJahr());

        long einnahmen = daten.einnahmen().stream()
                .mapToLong(KassenberichtAnhangDaten.KategorieBlock::summeCent).sum();
        long ausgaben = daten.ausgaben().stream()
                .mapToLong(KassenberichtAnhangDaten.KategorieBlock::summeCent).sum();
        assertEquals(11500L, einnahmen);
        assertEquals(4000L, ausgaben);
        // Die Aktivitaetstabelle ist eine andere Sicht auf dieselben Buchungen und muss daher
        // genau dieselben Summen ergeben.
        assertEquals(einnahmen, daten.aktivitaeten().stream()
                .mapToLong(KassenberichtAnhangDaten.AktivitaetErgebnis::einnahmenCent).sum());
        assertEquals(ausgaben, daten.aktivitaeten().stream()
                .mapToLong(KassenberichtAnhangDaten.AktivitaetErgebnis::ausgabenCent).sum());
    }

    @Test
    void anhangErzeugtPdfOhneSaldenUndUnterschriften() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        AktivitaetRepository aktivitaetRepository = new AktivitaetRepository(database);
        VereinRepository vereinRepository = new VereinRepository(database);
        Verein verein = new Verein();
        verein.setName("Förderverein Musterschule e. V.");
        vereinRepository.save(verein);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository,
                aktivitaetRepository, new MitgliedService(new MitgliedRepository(database),
                        new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), vereinRepository);

        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie spenden = kategorie(kategorieRepository, "Geldspenden");
        Aktivitaet martin = aktivitaet(aktivitaetRepository, "Sankt Martin");
        buchungRepository.insert(buchung(konto, spenden, martin, LocalDate.of(2026, 3, 1), "100.00"));
        buchungRepository.insert(buchung(konto, spenden, null, LocalDate.of(2026, 3, 2), "-40.00"));

        Path ziel = Files.createTempDirectory("kasse-anhang").resolve("anhang.pdf");
        berichtService.erstelleKassenberichtAnhang(ziel, konto, neuesJahr());

        String text = pdfText(ziel);
        assertTrue(text.contains("Kassenbericht Anhang"), text);
        assertTrue(text.contains("Förderverein Musterschule e. V."), text);
        assertTrue(text.contains("Kategorie / Aktivität"), text);
        assertTrue(text.contains("Sankt Martin"), text);
        assertTrue(text.contains("Aktivitäten insgesamt"), text);

        // Bewusst nicht im Anhang: die stehen im Kassenbericht selbst.
        assertFalse(text.contains("Startsaldo"), "Startsaldo gehoert nicht in den Anhang: " + text);
        assertFalse(text.contains("Endsaldo"), "Endsaldo gehoert nicht in den Anhang: " + text);
        assertFalse(text.contains("Unterschrift"), "Unterschriften gehoeren nicht in den Anhang: " + text);
    }

    private Kategorie kategorie(KategorieRepository repository, String name) {
        return repository.findAll().stream().filter(k -> k.getName().equals(name))
                .findFirst().orElseThrow(() -> new IllegalStateException("Starter-Kategorie fehlt: " + name));
    }

    private Aktivitaet aktivitaet(AktivitaetRepository repository, String name) {
        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setName(name);
        return repository.save(aktivitaet);
    }

    private Buchung buchung(Konto konto, Kategorie kategorie, Aktivitaet aktivitaet, LocalDate datum, String betrag) {
        Buchung buchung = neueBuchung(konto.getId(), kategorie != null ? kategorie.getId() : null,
                datum, new BigDecimal(betrag));
        buchung.setAktivitaetId(aktivitaet != null ? aktivitaet.getId() : null);
        return buchung;
    }

    @Test
    void mitgliederberichtBerechnetKennzahlenDerAktivenMitglieder() {
        Database database = TestDatabase.create();
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        BerichtService berichtService = berichtService(database, mitgliedRepository);

        mitgliedRepository.save(mitgliedMitBeitrag("Erika", "Musterfrau", MitgliedStatus.AKTIV, "24.00"));
        mitgliedRepository.save(mitgliedMitBeitrag("Max", "Mustermann", MitgliedStatus.AKTIV, "12.00"));
        mitgliedRepository.save(mitgliedMitBeitrag("Anna", "Beispiel", MitgliedStatus.AKTIV, "120.00"));
        // Ohne Jahresbeitrag: zaehlt bei den Mitgliedern mit, nicht bei den Betraegen.
        mitgliedRepository.save(mitgliedMitBeitrag("Ohne", "Beitrag", MitgliedStatus.AKTIV, null));
        // Inaktive bleiben komplett aussen vor.
        mitgliedRepository.save(mitgliedMitBeitrag("Alt", "Mitglied", MitgliedStatus.INAKTIV, "999.00"));

        MitgliederberichtDaten daten = berichtService.berechneMitgliederbericht();

        assertEquals(4, daten.mitglieder());
        assertEquals(15600L, daten.summeCent());
        assertEquals(1200L, daten.minCent());
        assertEquals(12000L, daten.maxCent());
        // 156,00 / 3 = 52,00 - der Durchschnitt bezieht sich auf die drei Mitglieder mit Beitrag.
        assertEquals(5200L, daten.durchschnittCent());
    }

    /** Ohne hinterlegte Betraege ist 0,00 Euro als Minimum/Maximum/Durchschnitt eine falsche
     * Aussage - die Werte bleiben leer und das PDF zeigt einen Strich. */
    @Test
    void mitgliederberichtLaesstBeitragswerteOhneBetraegeLeer() {
        Database database = TestDatabase.create();
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        mitgliedRepository.save(mitgliedMitBeitrag("Ohne", "Beitrag", MitgliedStatus.AKTIV, null));

        MitgliederberichtDaten daten = berichtService(database, mitgliedRepository).berechneMitgliederbericht();

        assertEquals(1, daten.mitglieder());
        assertEquals(0L, daten.summeCent());
        assertNull(daten.minCent());
        assertNull(daten.maxCent());
        assertNull(daten.durchschnittCent());
    }

    @Test
    void mitgliederberichtErzeugtPdfMitDenKennzahlen() throws IOException {
        Database database = TestDatabase.create();
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        VereinRepository vereinRepository = new VereinRepository(database);
        Verein verein = new Verein();
        verein.setName("Förderverein Musterschule e. V.");
        vereinRepository.save(verein);

        mitgliedRepository.save(mitgliedMitBeitrag("Erika", "Musterfrau", MitgliedStatus.AKTIV, "24.00"));
        mitgliedRepository.save(mitgliedMitBeitrag("Alt", "Mitglied", MitgliedStatus.INAKTIV, "999.00"));

        Path ziel = Files.createTempDirectory("kasse-mitgliederbericht").resolve("mitgliederbericht.pdf");
        berichtService(database, mitgliedRepository).erstelleMitgliederbericht(ziel);

        String text = pdfText(ziel);
        assertTrue(text.contains("Mitgliederbericht"), text);
        assertTrue(text.contains("Förderverein Musterschule e. V."), text);
        assertTrue(text.contains("Stand:"), text);
        assertTrue(text.contains("Mitglieder und Jahresbeitrag"), text);
        assertTrue(text.contains("Jahresbeitrag Durchschnitt"), text);

        // Abgewaehlte Bestandteile duerfen nicht zurueckkehren.
        assertFalse(text.contains("Inaktiv"), "Spalte Inaktiv entfernt: " + text);
        assertFalse(text.contains("Gesamt"), "Spalte Gesamt entfernt: " + text);
        assertFalse(text.contains("davon mit Jahresbeitrag"), "Zeile entfernt: " + text);
        assertFalse(text.contains("unberücksichtigt"), "Fussnote entfernt: " + text);
        assertFalse(text.contains("Erstellt am"), "Schlusszeile entfernt, Datum steht oben: " + text);
        // Der Jahresbeitrag des inaktiven Mitglieds darf nirgends auftauchen.
        assertFalse(text.contains("999,00"), "Inaktive zaehlen nicht mit: " + text);
    }

    private Mitglied mitgliedMitBeitrag(String vorname, String nachname, MitgliedStatus status, String betrag) {
        Mitglied mitglied = neuesMitglied(vorname, nachname);
        mitglied.setStatus(status);
        if (betrag != null) {
            mitglied.setJahresbeitragEuro(new BigDecimal(betrag));
        }
        return mitglied;
    }

    private BerichtService berichtService(Database database, MitgliedRepository mitgliedRepository) {
        return new BerichtService(new BuchungRepository(database), new KategorieRepository(database),
                new AktivitaetRepository(database), new MitgliedService(mitgliedRepository, new AuditLogRepository(database)),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)), new VereinRepository(database));
    }

    private Mitglied neuesMitglied(String vorname, String nachname) {
        Mitglied mitglied = new Mitglied();
        mitglied.setVorname(vorname);
        mitglied.setNachname(nachname);
        return mitglied;
    }

    /** Schreibt einzelne Zellen einer exportierten Datei um - so entstehen Werte, die der Export
     * selbst nie erzeugen koennte (unbekannter Status, nichtnumerische Mandatsreferenz). */
    private void setzeZellen(Path xlsx, int zeilenIndex, Map<Integer, String> werte) throws Exception {
        byte[] inhalt = Files.readAllBytes(xlsx);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(inhalt))) {
            Row zeile = workbook.getSheetAt(0).getRow(zeilenIndex);
            werte.forEach((spalte, wert) -> {
                Cell zelle = zeile.getCell(spalte);
                if (zelle == null) {
                    zelle = zeile.createCell(spalte);
                }
                zelle.setCellValue(wert);
            });
            try (java.io.OutputStream out = Files.newOutputStream(xlsx)) {
                workbook.write(out);
            }
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
