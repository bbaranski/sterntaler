package de.foerderverein.kasse.persistence;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Beleg;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Konto;

/** Prueft die einmalige Migration bestehender Beleg-BLOBs auf Dateien. Die "alte" Zeile wird per
 * rohem JDBC eingefuegt, solange die inhalt-Spalte (aus V12) noch existiert - genau der Zustand,
 * in dem sich eine seit vor der Umstellung bestehende, echte Datenbank beim ersten Start nach
 * dieser Aenderung befindet. */
class BelegDateiMigrationTest {

    @Test
    void migriertBestehendenBlobAufDateiUndEntferntInhaltSpalte() throws SQLException, IOException {
        Database database = TestDatabase.create();
        Konto konto = new KontoRepository(database).save(neuesKonto());
        Buchung buchung = neueBuchung(konto.getId());
        new BuchungRepository(database).insert(buchung);

        byte[] inhalt = {1, 2, 3, 4};
        fuegeAlteBelegZeileEin(database, buchung.getId(), "Rechnung.pdf", "pdf", inhalt);

        Path belegeVerzeichnis = Files.createTempDirectory("kasse-test-belege");
        BelegDateiMigration.migriereFallsNoetig(database, belegeVerzeichnis);

        Beleg geladen = new BelegRepository(database).findByBuchungId(buchung.getId()).orElseThrow();
        assertEquals(buchung.getId() + "_Rechnung.pdf", geladen.getSpeicherpfad());

        Path erwarteteDatei = belegeVerzeichnis.resolve(geladen.getSpeicherpfad());
        assertTrue(Files.exists(erwarteteDatei));
        assertArrayEquals(inhalt, Files.readAllBytes(erwarteteDatei));

        assertFalse(hatSpalte(database, "inhalt"));
    }

    @Test
    void zweiterAufrufIstEinSicheresNoop() throws SQLException, IOException {
        Database database = TestDatabase.create();
        Path belegeVerzeichnis = Files.createTempDirectory("kasse-test-belege");

        BelegDateiMigration.migriereFallsNoetig(database, belegeVerzeichnis);
        BelegDateiMigration.migriereFallsNoetig(database, belegeVerzeichnis);

        assertFalse(hatSpalte(database, "inhalt"));
    }

    private void fuegeAlteBelegZeileEin(Database database, long buchungId, String dateiname, String dateityp,
                                         byte[] inhalt) throws SQLException {
        String sql = "INSERT INTO beleg (buchung_id, dateiname, dateityp, inhalt, hochgeladen_am) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, buchungId);
            ps.setString(2, dateiname);
            ps.setString(3, dateityp);
            ps.setBytes(4, inhalt);
            ps.setString(5, java.time.LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    private boolean hatSpalte(Database database, String spalte) throws SQLException {
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(beleg)")) {
            while (rs.next()) {
                if (spalte.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
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
