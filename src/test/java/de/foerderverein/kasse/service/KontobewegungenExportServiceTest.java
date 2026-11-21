package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class KontobewegungenExportServiceTest {

    /** Waechter gegen vergessene Spalten: zu jedem Feld von Buchung muss es eine Spalte geben,
     * und zu jeder Spalte ein Feld. Schlaegt fehl, sobald Buchung erweitert wird, ohne den Export
     * nachzuziehen - genau so ist geschaeftsjahrId nach seiner Einfuehrung durchgerutscht. Die
     * Zuordnung steht hier bewusst ausgeschrieben: ein Abgleich "Anzahl == Anzahl" wuerde ein
     * entferntes gegen ein neues Feld aufrechnen. */
    @Test
    void jedesFeldVonBuchungHatEineSpalteImExport() throws Exception {
        Map<String, String> spalteJeFeld = new LinkedHashMap<>();
        spalteJeFeld.put("id", "Id");
        spalteJeFeld.put("buchungsdatum", "Datum");
        spalteJeFeld.put("geschaeftsjahrId", "Geschäftsjahr");
        spalteJeFeld.put("wertstellung", "Wertstellung");
        spalteJeFeld.put("kontoId", "Konto");
        spalteJeFeld.put("kontoauszug", "Kontoauszug");
        spalteJeFeld.put("betragCent", "Betrag");
        spalteJeFeld.put("waehrung", "Währung");
        spalteJeFeld.put("kategorieId", "Kategorie");
        spalteJeFeld.put("aktivitaetId", "Aktivität");
        spalteJeFeld.put("verwendungszweck", "Verwendungszweck");
        spalteJeFeld.put("rechnungsnummer", "Rechnungsnummer");
        spalteJeFeld.put("gegenkontoIban", "Gegenkonto IBAN");
        spalteJeFeld.put("gegenkontoName", "Gegenkonto Name");
        spalteJeFeld.put("quelle", "Quelle");
        spalteJeFeld.put("hbciImportHash", "HBCI-Import-Hash");
        spalteJeFeld.put("storniert", "Storniert");
        spalteJeFeld.put("stornoReferenzId", "Storno-Referenz-Id");
        spalteJeFeld.put("erstelltVon", "Erstellt von");
        spalteJeFeld.put("erstelltAm", "Erstellt am");
        spalteJeFeld.put("geaendertAm", "Geändert am");

        List<String> felder = Arrays.stream(Buchung.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()) && !f.isSynthetic())
                .map(Field::getName)
                .sorted()
                .toList();
        assertEquals(felder, spalteJeFeld.keySet().stream().sorted().toList(),
                "Buchung und Exportspalten laufen auseinander");

        Database database = TestDatabase.create();
        Path ziel = Files.createTempDirectory("kasse-export-spalten").resolve("buchungen.xlsx");
        exportService(database).exportiereAlleBuchungenAlsExcel(ziel);

        try (XSSFWorkbook workbook = new XSSFWorkbook(ziel.toFile())) {
            Row kopfzeile = workbook.getSheet("Kontobewegungen").getRow(0);
            List<String> kopf = new ArrayList<>();
            for (int i = 0; i < kopfzeile.getLastCellNum(); i++) {
                kopf.add(kopfzeile.getCell(i).getStringCellValue());
            }
            assertEquals(spalteJeFeld.values().stream().sorted().toList(), kopf.stream().sorted().toList(),
                    "Kopfzeile der Exportdatei weicht von der Zuordnung ab");
        }
    }

    private KontobewegungenExportService exportService(Database database) {
        return new KontobewegungenExportService(new BuchungRepository(database),
                new KontoRepository(database), new KategorieRepository(database),
                new AktivitaetRepository(database),
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)));
    }

    @Test
    void exportiereAlleBuchungenAlsExcelSchreibtAlleFelder() throws Exception {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        AktivitaetRepository aktivitaetRepository = new AktivitaetRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        Kategorie kategorie = kategorieRepository.findAll().get(0);
        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setName("Sankt Martin");
        aktivitaetRepository.save(aktivitaet);

        Buchung buchung = neueBuchung(konto.getId());
        buchung.setKategorieId(kategorie.getId());
        buchung.setAktivitaetId(aktivitaet.getId());
        buchung.setWertstellung(LocalDate.of(2026, 3, 3));
        buchung.setHbciImportHash("hash-123");
        buchung.setErstelltVon("tester");
        buchungRepository.insert(buchung);
        long buchungId = buchung.getId();
        // erstellt_am wird beim Insert serverseitig mit dem tatsaechlichen Einfuegezeitpunkt
        // gesetzt (nicht aus dem Java-Objekt uebernommen) - die echte Buchung wird daher nach
        // dem Insert neu geladen, um den tatsaechlich persistierten Zeitstempel zu kennen.
        buchung = buchungRepository.findAll().stream()
                .filter(b -> b.getId() == buchungId).findFirst().orElseThrow();

        Geschaeftsjahr jahr = new Geschaeftsjahr();
        jahr.setBezeichnung("2026");
        jahr.setStartDatum(LocalDate.of(2026, 1, 1));
        jahr.setEndDatum(LocalDate.of(2026, 12, 31));
        new GeschaeftsjahrRepository(database).save(jahr);

        Buchung stornoBuchung = neueBuchung(konto.getId());
        stornoBuchung.setStornoReferenzId(buchung.getId());
        buchungRepository.insert(stornoBuchung);
        buchungRepository.storno(stornoBuchung.getId());

        KontobewegungenExportService exportService = new KontobewegungenExportService(buchungRepository,
                new KontoRepository(database), kategorieRepository, aktivitaetRepository,
                new GeschaeftsjahrService(new GeschaeftsjahrRepository(database)));

        Path ziel = Files.createTempDirectory("kasse-export-test").resolve("buchungen.xlsx");
        exportService.exportiereAlleBuchungenAlsExcel(ziel);

        assertTrue(Files.exists(ziel));
        try (XSSFWorkbook workbook = new XSSFWorkbook(ziel.toFile())) {
            Sheet sheet = workbook.getSheet("Kontobewegungen");
            Row kopfzeile = sheet.getRow(0);
            String[] erwarteteSpalten = {"Id", "Datum", "Geschäftsjahr", "Wertstellung", "Konto", "Kontoauszug",
                    "Betrag", "Währung", "Kategorie", "Aktivität", "Verwendungszweck", "Rechnungsnummer",
                    "Gegenkonto IBAN", "Gegenkonto Name", "Quelle", "HBCI-Import-Hash", "Storniert",
                    "Storno-Referenz-Id", "Erstellt von", "Erstellt am", "Geändert am"};
            for (int i = 0; i < erwarteteSpalten.length; i++) {
                assertEquals(erwarteteSpalten[i], kopfzeile.getCell(i).getStringCellValue());
            }

            // findAll() sortiert absteigend nach Datum/Id - bei gleichem Datum steht die zuletzt
            // angelegte (hier die Storno-)Buchung zuerst.
            Row stornoZeile = sheet.getRow(1);
            assertEquals("Ja", stornoZeile.getCell(16).getStringCellValue());
            assertEquals(buchung.getId(), (long) stornoZeile.getCell(17).getNumericCellValue());

            Row datenzeile = sheet.getRow(2);
            assertEquals(buchung.getId(), (long) datenzeile.getCell(0).getNumericCellValue());
            assertEquals("01.03.2026", datenzeile.getCell(1).getStringCellValue());
            // Ohne Uebersteuerung am Buchungssatz das Jahr, in dessen Zeitraum das Datum faellt.
            assertEquals("2026", datenzeile.getCell(2).getStringCellValue());
            assertEquals("03.03.2026", datenzeile.getCell(3).getStringCellValue());
            assertEquals("Vereinskonto", datenzeile.getCell(4).getStringCellValue());
            assertEquals(42.50, datenzeile.getCell(6).getNumericCellValue());
            assertEquals("EUR", datenzeile.getCell(7).getStringCellValue());
            assertEquals(kategorie.getName(), datenzeile.getCell(8).getStringCellValue());
            assertEquals("Sankt Martin", datenzeile.getCell(9).getStringCellValue());
            assertEquals("Testverwendungszweck", datenzeile.getCell(10).getStringCellValue());
            assertEquals("MANUELL", datenzeile.getCell(14).getStringCellValue());
            assertEquals("hash-123", datenzeile.getCell(15).getStringCellValue());
            assertEquals("Nein", datenzeile.getCell(16).getStringCellValue());
            assertEquals("tester", datenzeile.getCell(18).getStringCellValue());
            DateTimeFormatter zeitpunktFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            assertEquals(buchung.getErstelltAm().format(zeitpunktFormat), datenzeile.getCell(19).getStringCellValue());
            // geaendert_am bleibt bei einer frisch angelegten Buchung unveraendert leer - erst
            // BuchungService.aktualisiere(...) setzt es, nicht BuchungRepository.insert(...).
            assertEquals("", datenzeile.getCell(20).getStringCellValue());
        }
    }

    /** Der Export eines Geschaeftsjahres muss dieselbe Zuordnung treffen wie die Spalte
     * "Geschaeftsjahr": Zeitraum, aber eine Uebersteuerung am Buchungssatz gewinnt. Deshalb hier
     * drei Buchungen - eine im Zeitraum, eine ausserhalb und eine, die per geschaeftsjahrId ins
     * Jahr geholt wird, obwohl ihr Datum daneben liegt. */
    @Test
    void exportEinesGeschaeftsjahresNimmtNurDessenBuchungen() throws Exception {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        GeschaeftsjahrRepository geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());

        Geschaeftsjahr jahr2025 = geschaeftsjahrRepository.save(
                neuesGeschaeftsjahr("2025", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        Geschaeftsjahr jahr2026 = geschaeftsjahrRepository.save(
                neuesGeschaeftsjahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

        Buchung imJahr = neueBuchung(konto.getId());
        buchungRepository.insert(imJahr);

        Buchung imVorjahr = neueBuchung(konto.getId());
        imVorjahr.setBuchungsdatum(LocalDate.of(2025, 6, 1));
        buchungRepository.insert(imVorjahr);

        Buchung uebersteuert = neueBuchung(konto.getId());
        uebersteuert.setBuchungsdatum(LocalDate.of(2025, 12, 31));
        uebersteuert.setGeschaeftsjahrId(jahr2026.getId());
        buchungRepository.insert(uebersteuert);

        Path ziel = Files.createTempDirectory("kasse-export-jahr").resolve("buchungen-2026.xlsx");
        exportService(database).exportiereBuchungenEinesGeschaeftsjahresAlsExcel(ziel, jahr2026);

        try (XSSFWorkbook workbook = new XSSFWorkbook(ziel.toFile())) {
            Sheet sheet = workbook.getSheet("Kontobewegungen");
            List<Long> ids = new ArrayList<>();
            for (int zeile = 1; zeile <= sheet.getLastRowNum(); zeile++) {
                ids.add((long) sheet.getRow(zeile).getCell(0).getNumericCellValue());
                assertEquals("2026", sheet.getRow(zeile).getCell(2).getStringCellValue());
            }
            assertEquals(List.of(imJahr.getId(), uebersteuert.getId()).stream().sorted().toList(),
                    ids.stream().sorted().toList(),
                    "Export des Geschäftsjahres 2026 enthält die falschen Buchungen");
        }

        // Im Vorjahr bleibt genau die Buchung, deren Datum dort liegt - ohne die uebersteuerte.
        Path zielVorjahr = Files.createTempDirectory("kasse-export-vorjahr").resolve("buchungen-2025.xlsx");
        exportService(database).exportiereBuchungenEinesGeschaeftsjahresAlsExcel(zielVorjahr, jahr2025);
        try (XSSFWorkbook workbook = new XSSFWorkbook(zielVorjahr.toFile())) {
            Sheet sheet = workbook.getSheet("Kontobewegungen");
            assertEquals(1, sheet.getLastRowNum(), "2025 enthält nicht genau eine Buchung");
            assertEquals(imVorjahr.getId(), (long) sheet.getRow(1).getCell(0).getNumericCellValue());
        }

        // Gegenprobe: ueber alle Kontobewegungen sind es wieder alle drei.
        Path alle = Files.createTempDirectory("kasse-export-alle").resolve("buchungen.xlsx");
        exportService(database).exportiereAlleBuchungenAlsExcel(alle);
        try (XSSFWorkbook workbook = new XSSFWorkbook(alle.toFile())) {
            assertEquals(3, workbook.getSheet("Kontobewegungen").getLastRowNum(),
                    "Der Export über alle Kontobewegungen darf nicht filtern");
        }
    }

    private Geschaeftsjahr neuesGeschaeftsjahr(String bezeichnung, LocalDate start, LocalDate ende) {
        Geschaeftsjahr jahr = new Geschaeftsjahr();
        jahr.setBezeichnung(bezeichnung);
        jahr.setStartDatum(start);
        jahr.setEndDatum(ende);
        return jahr;
    }

    private Konto neuesKonto() {
        Konto konto = new Konto();
        konto.setBezeichnung("Vereinskonto");
        konto.setIban("DE02120300000000202051");
        return konto;
    }

    private Buchung neueBuchung(Long kontoId) {
        Buchung buchung = new Buchung();
        buchung.setKontoId(kontoId);
        buchung.setBuchungsdatum(LocalDate.of(2026, 3, 1));
        buchung.setBetragEuro(new BigDecimal("42.50"));
        buchung.setVerwendungszweck("Testverwendungszweck");
        buchung.setQuelle(BuchungsQuelle.MANUELL);
        return buchung;
    }
}
