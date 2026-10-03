package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class KontobewegungenExportServiceTest {

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

        Buchung stornoBuchung = neueBuchung(konto.getId());
        stornoBuchung.setStornoReferenzId(buchung.getId());
        buchungRepository.insert(stornoBuchung);
        buchungRepository.storno(stornoBuchung.getId());

        KontobewegungenExportService exportService = new KontobewegungenExportService(buchungRepository,
                new KontoRepository(database), kategorieRepository, aktivitaetRepository);

        Path ziel = Files.createTempDirectory("kasse-export-test").resolve("buchungen.xlsx");
        exportService.exportiereAlleBuchungenAlsExcel(ziel);

        assertTrue(Files.exists(ziel));
        try (XSSFWorkbook workbook = new XSSFWorkbook(ziel.toFile())) {
            Sheet sheet = workbook.getSheet("Kontobewegungen");
            Row kopfzeile = sheet.getRow(0);
            String[] erwarteteSpalten = {"Id", "Datum", "Wertstellung", "Konto", "Kontoauszug", "Betrag", "Währung",
                    "Kategorie", "Aktivität", "Verwendungszweck", "Rechnungsnummer", "Gegenkonto IBAN",
                    "Gegenkonto Name", "Quelle", "HBCI-Import-Hash", "Storniert", "Storno-Referenz-Id",
                    "Erstellt von", "Erstellt am", "Geändert am"};
            for (int i = 0; i < erwarteteSpalten.length; i++) {
                assertEquals(erwarteteSpalten[i], kopfzeile.getCell(i).getStringCellValue());
            }

            // findAll() sortiert absteigend nach Datum/Id - bei gleichem Datum steht die zuletzt
            // angelegte (hier die Storno-)Buchung zuerst.
            Row stornoZeile = sheet.getRow(1);
            assertEquals("Ja", stornoZeile.getCell(15).getStringCellValue());
            assertEquals(buchung.getId(), (long) stornoZeile.getCell(16).getNumericCellValue());

            Row datenzeile = sheet.getRow(2);
            assertEquals(buchung.getId(), (long) datenzeile.getCell(0).getNumericCellValue());
            assertEquals("01.03.2026", datenzeile.getCell(1).getStringCellValue());
            assertEquals("03.03.2026", datenzeile.getCell(2).getStringCellValue());
            assertEquals("Vereinskonto", datenzeile.getCell(3).getStringCellValue());
            assertEquals(42.50, datenzeile.getCell(5).getNumericCellValue());
            assertEquals("EUR", datenzeile.getCell(6).getStringCellValue());
            assertEquals(kategorie.getName(), datenzeile.getCell(7).getStringCellValue());
            assertEquals("Sankt Martin", datenzeile.getCell(8).getStringCellValue());
            assertEquals("Testverwendungszweck", datenzeile.getCell(9).getStringCellValue());
            assertEquals("MANUELL", datenzeile.getCell(13).getStringCellValue());
            assertEquals("hash-123", datenzeile.getCell(14).getStringCellValue());
            assertEquals("Nein", datenzeile.getCell(15).getStringCellValue());
            assertEquals("tester", datenzeile.getCell(17).getStringCellValue());
            DateTimeFormatter zeitpunktFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            assertEquals(buchung.getErstelltAm().format(zeitpunktFormat), datenzeile.getCell(18).getStringCellValue());
            // geaendert_am bleibt bei einer frisch angelegten Buchung unveraendert leer - erst
            // BuchungService.aktualisiere(...) setzt es, nicht BuchungRepository.insert(...).
            assertEquals("", datenzeile.getCell(19).getStringCellValue());
        }
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
