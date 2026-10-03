package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.foerderverein.kasse.domain.Beleg;

public class BelegRepository {

    private final Database database;

    public BelegRepository(Database database) {
        this.database = database;
    }

    /** Alle Belege, keyed by buchungId. */
    public Map<Long, Beleg> findAll() {
        String sql = "SELECT id, buchung_id, dateiname, dateityp, speicherpfad, hochgeladen_am FROM beleg";
        Map<Long, Beleg> result = new HashMap<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Beleg beleg = map(rs);
                result.put(beleg.getBuchungId(), beleg);
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Belege konnten nicht geladen werden", e);
        }
    }

    public Optional<Beleg> findByBuchungId(long buchungId) {
        String sql = "SELECT id, buchung_id, dateiname, dateityp, speicherpfad, hochgeladen_am "
                + "FROM beleg WHERE buchung_id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, buchungId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            throw new PersistenceException("Beleg konnte nicht geladen werden", e);
        }
    }

    /** Legt einen neuen Beleg an oder ersetzt einen bereits vorhandenen zu derselben Buchung -
     * der Dispatch-Schluessel ist hier bewusst "existiert schon ein Beleg zu dieser buchungId",
     * nicht wie sonst ueblich "ist die eigene id null", da ein neu hochgeladener Beleg beim
     * Ersetzen ebenfalls noch keine eigene id hat. */
    public Beleg speichern(Beleg beleg) {
        Optional<Beleg> vorhanden = findByBuchungId(beleg.getBuchungId());
        if (vorhanden.isPresent()) {
            beleg.setId(vorhanden.get().getId());
            return update(beleg);
        }
        return insert(beleg);
    }

    private Beleg insert(Beleg beleg) {
        String sql = """
                INSERT INTO beleg (buchung_id, dateiname, dateityp, speicherpfad, hochgeladen_am)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, beleg.getBuchungId());
            ps.setString(2, beleg.getDateiname());
            ps.setString(3, beleg.getDateityp());
            ps.setString(4, beleg.getSpeicherpfad());
            ps.setString(5, beleg.getHochgeladenAm().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                beleg.setId(keys.getLong(1));
            }
            return beleg;
        } catch (SQLException e) {
            throw new PersistenceException("Beleg konnte nicht angelegt werden", e);
        }
    }

    private Beleg update(Beleg beleg) {
        String sql = "UPDATE beleg SET dateiname = ?, dateityp = ?, speicherpfad = ?, hochgeladen_am = ? WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, beleg.getDateiname());
            ps.setString(2, beleg.getDateityp());
            ps.setString(3, beleg.getSpeicherpfad());
            ps.setString(4, beleg.getHochgeladenAm().toString());
            ps.setLong(5, beleg.getId());
            ps.executeUpdate();
            return beleg;
        } catch (SQLException e) {
            throw new PersistenceException("Beleg konnte nicht aktualisiert werden", e);
        }
    }

    private Beleg map(ResultSet rs) throws SQLException {
        Beleg beleg = new Beleg();
        beleg.setId(rs.getLong("id"));
        beleg.setBuchungId(rs.getLong("buchung_id"));
        beleg.setDateiname(rs.getString("dateiname"));
        beleg.setDateityp(rs.getString("dateityp"));
        beleg.setSpeicherpfad(rs.getString("speicherpfad"));
        beleg.setHochgeladenAm(LocalDateTime.parse(rs.getString("hochgeladen_am")));
        return beleg;
    }
}
