package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.foerderverein.kasse.domain.Aktivitaet;

public class AktivitaetRepository {

    private final Database database;

    public AktivitaetRepository(Database database) {
        this.database = database;
    }

    public List<Aktivitaet> findAll() {
        String sql = "SELECT * FROM aktivitaet ORDER BY name";
        List<Aktivitaet> result = new ArrayList<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Aktivitaeten konnten nicht geladen werden", e);
        }
    }

    public Optional<Aktivitaet> findById(long id) {
        String sql = "SELECT * FROM aktivitaet WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Aktivitaet konnte nicht geladen werden", e);
        }
    }

    public Aktivitaet save(Aktivitaet aktivitaet) {
        return aktivitaet.getId() == null ? insert(aktivitaet) : update(aktivitaet);
    }

    private Aktivitaet insert(Aktivitaet aktivitaet) {
        String sql = "INSERT INTO aktivitaet (name, notiz, aktiv) VALUES (?, ?, ?)";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, aktivitaet);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                aktivitaet.setId(keys.getLong(1));
            }
            return aktivitaet;
        } catch (SQLException e) {
            throw new PersistenceException("Aktivitaet konnte nicht angelegt werden", e);
        }
    }

    private Aktivitaet update(Aktivitaet aktivitaet) {
        String sql = "UPDATE aktivitaet SET name = ?, notiz = ?, aktiv = ? WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, aktivitaet);
            ps.setLong(4, aktivitaet.getId());
            ps.executeUpdate();
            return aktivitaet;
        } catch (SQLException e) {
            throw new PersistenceException("Aktivitaet konnte nicht aktualisiert werden", e);
        }
    }

    private void bind(PreparedStatement ps, Aktivitaet aktivitaet) throws SQLException {
        ps.setString(1, aktivitaet.getName());
        ps.setString(2, aktivitaet.getNotiz());
        ps.setInt(3, aktivitaet.isAktiv() ? 1 : 0);
    }

    private Aktivitaet map(ResultSet rs) throws SQLException {
        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setId(rs.getLong("id"));
        aktivitaet.setName(rs.getString("name"));
        aktivitaet.setNotiz(rs.getString("notiz"));
        aktivitaet.setAktiv(rs.getInt("aktiv") == 1);
        return aktivitaet;
    }
}
