package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;

public class MitgliedRepository {

    private final Database database;

    public MitgliedRepository(Database database) {
        this.database = database;
    }

    public List<Mitglied> findAll() {
        String sql = "SELECT * FROM mitglied ORDER BY nachname, vorname";
        List<Mitglied> result = new ArrayList<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Mitglieder konnten nicht geladen werden", e);
        }
    }

    public List<Mitglied> findAllAktiv() {
        String sql = "SELECT * FROM mitglied WHERE status = 'AKTIV' ORDER BY nachname, vorname";
        List<Mitglied> result = new ArrayList<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Mitglieder konnten nicht geladen werden", e);
        }
    }

    public Optional<Mitglied> findById(long id) {
        String sql = "SELECT * FROM mitglied WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Mitglied konnte nicht geladen werden", e);
        }
    }

    public Mitglied save(Mitglied mitglied) {
        return mitglied.getId() == null ? insert(mitglied) : update(mitglied);
    }

    private Mitglied insert(Mitglied mitglied) {
        String sql = """
                INSERT INTO mitglied (vorname, nachname, strasse, plz, ort, email, eintrittsdatum,
                       austrittsdatum, status, notiz)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, mitglied);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                mitglied.setId(keys.getLong(1));
            }
            return mitglied;
        } catch (SQLException e) {
            throw new PersistenceException("Mitglied konnte nicht angelegt werden", e);
        }
    }

    private Mitglied update(Mitglied mitglied) {
        String sql = """
                UPDATE mitglied SET vorname = ?, nachname = ?, strasse = ?, plz = ?, ort = ?, email = ?,
                       eintrittsdatum = ?, austrittsdatum = ?, status = ?, notiz = ?
                WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, mitglied);
            ps.setLong(11, mitglied.getId());
            ps.executeUpdate();
            return mitglied;
        } catch (SQLException e) {
            throw new PersistenceException("Mitglied konnte nicht aktualisiert werden", e);
        }
    }

    private void bind(PreparedStatement ps, Mitglied mitglied) throws SQLException {
        ps.setString(1, mitglied.getVorname());
        ps.setString(2, mitglied.getNachname());
        ps.setString(3, mitglied.getStrasse());
        ps.setString(4, mitglied.getPlz());
        ps.setString(5, mitglied.getOrt());
        ps.setString(6, mitglied.getEmail());
        ps.setString(7, mitglied.getEintrittsdatum() != null ? mitglied.getEintrittsdatum().toString() : null);
        ps.setString(8, mitglied.getAustrittsdatum() != null ? mitglied.getAustrittsdatum().toString() : null);
        ps.setString(9, mitglied.getStatus().name());
        ps.setString(10, mitglied.getNotiz());
    }

    private Mitglied map(ResultSet rs) throws SQLException {
        Mitglied mitglied = new Mitglied();
        mitglied.setId(rs.getLong("id"));
        mitglied.setVorname(rs.getString("vorname"));
        mitglied.setNachname(rs.getString("nachname"));
        mitglied.setStrasse(rs.getString("strasse"));
        mitglied.setPlz(rs.getString("plz"));
        mitglied.setOrt(rs.getString("ort"));
        mitglied.setEmail(rs.getString("email"));
        String eintritt = rs.getString("eintrittsdatum");
        mitglied.setEintrittsdatum(eintritt != null ? LocalDate.parse(eintritt) : null);
        String austritt = rs.getString("austrittsdatum");
        mitglied.setAustrittsdatum(austritt != null ? LocalDate.parse(austritt) : null);
        mitglied.setStatus(MitgliedStatus.valueOf(rs.getString("status")));
        mitglied.setNotiz(rs.getString("notiz"));
        return mitglied;
    }
}
