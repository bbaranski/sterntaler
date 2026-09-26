package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;

public class BuchungRepository {

    private final Database database;

    public BuchungRepository(Database database) {
        this.database = database;
    }

    public List<Buchung> findAll() {
        String sql = "SELECT * FROM buchung ORDER BY buchungsdatum DESC, id DESC";
        List<Buchung> result = new ArrayList<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Buchungen konnten nicht geladen werden", e);
        }
    }

    public boolean existsByHbciHash(long kontoId, String hash) {
        String sql = "SELECT 1 FROM buchung WHERE konto_id = ? AND hbci_import_hash = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, kontoId);
            ps.setString(2, hash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Duplikatspruefung fehlgeschlagen", e);
        }
    }

    /** Fuer MitgliedService.loeschen: ein Mitglied darf nur geloescht werden, wenn ihm keine
     * Buchung zugeordnet ist. */
    public boolean existsByMitgliedId(long mitgliedId) {
        String sql = "SELECT 1 FROM buchung WHERE mitglied_id = ? LIMIT 1";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, mitgliedId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Pruefung auf verknuepfte Buchungen fehlgeschlagen", e);
        }
    }

    public Buchung insert(Buchung buchung) {
        String sql = """
                INSERT INTO buchung (konto_id, kategorie_id, mitglied_id, aktivitaet_id, buchungsdatum,
                       wertstellung, betrag_cent, waehrung, verwendungszweck, gegenkonto_iban,
                       gegenkonto_name, quelle, hbci_import_hash, storniert, storno_referenz_id,
                       erstellt_von, erstellt_am)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, buchung.getKontoId());
            if (buchung.getKategorieId() != null) {
                ps.setLong(2, buchung.getKategorieId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            if (buchung.getMitgliedId() != null) {
                ps.setLong(3, buchung.getMitgliedId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            if (buchung.getAktivitaetId() != null) {
                ps.setLong(4, buchung.getAktivitaetId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, buchung.getBuchungsdatum().toString());
            ps.setString(6, buchung.getWertstellung() != null ? buchung.getWertstellung().toString() : null);
            ps.setLong(7, buchung.getBetragCent());
            ps.setString(8, buchung.getWaehrung());
            ps.setString(9, buchung.getVerwendungszweck());
            ps.setString(10, buchung.getGegenkontoIban());
            ps.setString(11, buchung.getGegenkontoName());
            ps.setString(12, buchung.getQuelle().name());
            ps.setString(13, buchung.getHbciImportHash());
            if (buchung.getStornoReferenzId() != null) {
                ps.setLong(14, buchung.getStornoReferenzId());
            } else {
                ps.setNull(14, java.sql.Types.INTEGER);
            }
            ps.setString(15, buchung.getErstelltVon());
            ps.setString(16, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                buchung.setId(keys.getLong(1));
            }
            return buchung;
        } catch (SQLException e) {
            throw new PersistenceException("Buchung konnte nicht angelegt werden", e);
        }
    }

    public void update(Buchung buchung) {
        String sql = """
                UPDATE buchung SET kategorie_id = ?, mitglied_id = ?, aktivitaet_id = ?, buchungsdatum = ?,
                       wertstellung = ?, betrag_cent = ?, verwendungszweck = ?, gegenkonto_iban = ?,
                       gegenkonto_name = ?, geaendert_am = ?
                WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (buchung.getKategorieId() != null) {
                ps.setLong(1, buchung.getKategorieId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            if (buchung.getMitgliedId() != null) {
                ps.setLong(2, buchung.getMitgliedId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            if (buchung.getAktivitaetId() != null) {
                ps.setLong(3, buchung.getAktivitaetId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setString(4, buchung.getBuchungsdatum().toString());
            ps.setString(5, buchung.getWertstellung() != null ? buchung.getWertstellung().toString() : null);
            ps.setLong(6, buchung.getBetragCent());
            ps.setString(7, buchung.getVerwendungszweck());
            ps.setString(8, buchung.getGegenkontoIban());
            ps.setString(9, buchung.getGegenkontoName());
            ps.setString(10, LocalDateTime.now().toString());
            ps.setLong(11, buchung.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Buchung konnte nicht aktualisiert werden", e);
        }
    }

    /** Kassenfuehrung verlangt Nachvollziehbarkeit - Buchungen werden storniert statt geloescht. */
    public void storno(long buchungId) {
        String sql = "UPDATE buchung SET storniert = 1, geaendert_am = ? WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().toString());
            ps.setLong(2, buchungId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Buchung konnte nicht storniert werden", e);
        }
    }

    private Buchung map(ResultSet rs) throws SQLException {
        Buchung buchung = new Buchung();
        buchung.setId(rs.getLong("id"));
        buchung.setKontoId(rs.getLong("konto_id"));
        long kategorieId = rs.getLong("kategorie_id");
        buchung.setKategorieId(rs.wasNull() ? null : kategorieId);
        long mitgliedId = rs.getLong("mitglied_id");
        buchung.setMitgliedId(rs.wasNull() ? null : mitgliedId);
        long aktivitaetId = rs.getLong("aktivitaet_id");
        buchung.setAktivitaetId(rs.wasNull() ? null : aktivitaetId);
        buchung.setBuchungsdatum(LocalDate.parse(rs.getString("buchungsdatum")));
        String wertstellung = rs.getString("wertstellung");
        buchung.setWertstellung(wertstellung != null ? LocalDate.parse(wertstellung) : null);
        buchung.setBetragCent(rs.getLong("betrag_cent"));
        buchung.setWaehrung(rs.getString("waehrung"));
        buchung.setVerwendungszweck(rs.getString("verwendungszweck"));
        buchung.setGegenkontoIban(rs.getString("gegenkonto_iban"));
        buchung.setGegenkontoName(rs.getString("gegenkonto_name"));
        buchung.setQuelle(BuchungsQuelle.valueOf(rs.getString("quelle")));
        buchung.setHbciImportHash(rs.getString("hbci_import_hash"));
        buchung.setStorniert(rs.getInt("storniert") == 1);
        long stornoRef = rs.getLong("storno_referenz_id");
        buchung.setStornoReferenzId(rs.wasNull() ? null : stornoRef);
        buchung.setErstelltVon(rs.getString("erstellt_von"));
        String erstelltAm = rs.getString("erstellt_am");
        buchung.setErstelltAm(erstelltAm != null ? LocalDateTime.parse(erstelltAm) : null);
        String geaendertAm = rs.getString("geaendert_am");
        buchung.setGeaendertAm(geaendertAm != null ? LocalDateTime.parse(geaendertAm) : null);
        return buchung;
    }
}
