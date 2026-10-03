package de.foerderverein.kasse.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Einmalige, automatische Migration bestehender Beleg-BLOBs (Spalte "inhalt") auf Dateien im
 * Belege-Verzeichnis neben der Datenbankdatei - urspruenglich wurden Belege direkt in der
 * Datenbank gespeichert, was die .db-Datei bei vielen/grossen Belegen stark anwachsen liess.
 * Laeuft bei jedem Programmstart (siehe AppContext.create) und ist dabei sicher wiederholbar:
 * ist die inhalt-Spalte bereits entfernt, ist die Migration bereits vollstaendig abgeschlossen
 * und es wird sofort zurueckgekehrt. Bewusst kein Flyway-SQL-Migrationsschritt (V22 legt nur die
 * neue Spalte an) - das Schreiben der Dateien laesst sich nicht in SQL ausdruecken, und die
 * inhalt-Spalte darf erst entfernt werden, nachdem alle Bytes sicher auf der Festplatte liegen. */
public final class BelegDateiMigration {

    private static final Logger LOG = LoggerFactory.getLogger(BelegDateiMigration.class);

    private BelegDateiMigration() {
    }

    public static void migriereFallsNoetig(Database database, Path belegeVerzeichnis) {
        try (Connection c = database.connection()) {
            if (!hatSpalte(c, "inhalt")) {
                return;
            }
            Files.createDirectories(belegeVerzeichnis);
            migriereBestehendeBelege(c, belegeVerzeichnis);
            if (keineUnmigriertenBelegeMehr(c)) {
                c.createStatement().execute("ALTER TABLE beleg DROP COLUMN inhalt");
                c.createStatement().execute("VACUUM");
                LOG.info("Beleg-Dateien erfolgreich nach {} migriert, inhalt-Spalte entfernt.", belegeVerzeichnis);
            }
        } catch (SQLException | IOException e) {
            throw new PersistenceException("Beleg-Dateien konnten nicht migriert werden", e);
        }
    }

    private static void migriereBestehendeBelege(Connection c, Path belegeVerzeichnis) throws SQLException, IOException {
        String select = "SELECT id, buchung_id, dateiname, inhalt FROM beleg "
                + "WHERE inhalt IS NOT NULL AND speicherpfad IS NULL";
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(select)) {
            while (rs.next()) {
                long id = rs.getLong("id");
                long buchungId = rs.getLong("buchung_id");
                String dateiname = rs.getString("dateiname");
                byte[] inhalt = rs.getBytes("inhalt");

                String speicherpfad = BelegDateiName.zielname(buchungId, dateiname);
                Files.write(belegeVerzeichnis.resolve(speicherpfad), inhalt);

                try (PreparedStatement update = c.prepareStatement("UPDATE beleg SET speicherpfad = ? WHERE id = ?")) {
                    update.setString(1, speicherpfad);
                    update.setLong(2, id);
                    update.executeUpdate();
                }
            }
        }
    }

    private static boolean keineUnmigriertenBelegeMehr(Connection c) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM beleg WHERE inhalt IS NOT NULL AND speicherpfad IS NULL")) {
            rs.next();
            return rs.getInt(1) == 0;
        }
    }

    private static boolean hatSpalte(Connection c, String spalte) throws SQLException {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(beleg)")) {
            while (rs.next()) {
                if (spalte.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }
}
