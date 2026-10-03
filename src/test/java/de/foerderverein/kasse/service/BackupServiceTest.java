package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class BackupServiceTest {

    @Test
    void sichereDatenbankErzeugtOeffenbareKonsistenteKopie() throws IOException, java.sql.SQLException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        Buchung buchung = neueBuchung(konto.getId());
        buchungRepository.insert(buchung);

        BackupService backupService = new BackupService(database, buchungRepository, new KontoRepository(database),
                new KategorieRepository(database), Files.createTempDirectory("kasse-backup-belege-test"));

        Path ziel = Files.createTempDirectory("kasse-backup-test").resolve("sicherung.db");
        backupService.sichereDatenbank(ziel);

        assertTrue(Files.exists(ziel));
        assertTrue(Files.size(ziel) > 0);

        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + ziel.toAbsolutePath());
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM buchung")) {
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void exportiereAlleBuchungenSchreibtCsv() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        buchungRepository.insert(neueBuchung(konto.getId()));

        BackupService backupService = new BackupService(database, buchungRepository, new KontoRepository(database),
                new KategorieRepository(database), Files.createTempDirectory("kasse-backup-belege-test"));

        Path ziel = Files.createTempDirectory("kasse-backup-test").resolve("buchungen.csv");
        backupService.exportiereAlleBuchungen(ziel);

        List<String> zeilen = Files.readAllLines(ziel);
        assertTrue(zeilen.get(0).startsWith("Datum;Konto;Kontoauszug;Betrag"));
        assertTrue(zeilen.stream().anyMatch(z -> z.contains("Vereinskonto") && z.contains("Testverwendungszweck")));
    }

    @Test
    void exportiereAlleBuchungenAlsExcelSchreibtArbeitsblatt() throws Exception {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        buchungRepository.insert(neueBuchung(konto.getId()));

        BackupService backupService = new BackupService(database, buchungRepository, new KontoRepository(database),
                new KategorieRepository(database), Files.createTempDirectory("kasse-backup-belege-test"));

        Path ziel = Files.createTempDirectory("kasse-backup-test").resolve("buchungen.xlsx");
        backupService.exportiereAlleBuchungenAlsExcel(ziel);

        assertTrue(Files.exists(ziel));
        try (XSSFWorkbook workbook = new XSSFWorkbook(ziel.toFile())) {
            Sheet sheet = workbook.getSheet("Kontobewegungen");
            Row kopfzeile = sheet.getRow(0);
            assertEquals("Datum", kopfzeile.getCell(0).getStringCellValue());
            assertEquals("Konto", kopfzeile.getCell(1).getStringCellValue());
            assertEquals("Kontoauszug", kopfzeile.getCell(2).getStringCellValue());
            assertEquals("Betrag", kopfzeile.getCell(3).getStringCellValue());

            Row datenzeile = sheet.getRow(1);
            assertEquals("Vereinskonto", datenzeile.getCell(1).getStringCellValue());
            assertEquals(42.50, datenzeile.getCell(3).getNumericCellValue());
            assertEquals("Testverwendungszweck", datenzeile.getCell(5).getStringCellValue());
        }
    }

    @Test
    void sichereDatenbankKopiertAuchDasBelegeVerzeichnis() throws IOException {
        Database database = TestDatabase.create();
        BuchungRepository buchungRepository = new BuchungRepository(database);
        Konto konto = new KontoRepository(database).save(neuesKonto());
        buchungRepository.insert(neueBuchung(konto.getId()));

        Path quellBelegeVerzeichnis = Files.createTempDirectory("kasse-backup-belege-test");
        Files.writeString(quellBelegeVerzeichnis.resolve("1_Rechnung.pdf"), "Testinhalt");

        BackupService backupService = new BackupService(database, buchungRepository, new KontoRepository(database),
                new KategorieRepository(database), quellBelegeVerzeichnis);

        Path ziel = Files.createTempDirectory("kasse-backup-test").resolve("sicherung.db");
        backupService.sichereDatenbank(ziel);

        Path zielBelegeVerzeichnis = ziel.getParent().resolve("sicherung-belege");
        assertTrue(Files.exists(zielBelegeVerzeichnis.resolve("1_Rechnung.pdf")));
        assertEquals("Testinhalt", Files.readString(zielBelegeVerzeichnis.resolve("1_Rechnung.pdf")));
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
