package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.foerderverein.kasse.domain.Briefvorlage;

public class BriefvorlageRepository {

    private final Database database;

    public BriefvorlageRepository(Database database) {
        this.database = database;
    }

    public List<Briefvorlage> findAll() {
        String sql = "SELECT id, name, betreff, dateiname, speicherpfad, brieftext, signatur_name, signatur_rolle "
                + "FROM briefvorlage ORDER BY name";
        List<Briefvorlage> result = new ArrayList<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Serienbriefvorlagen konnten nicht geladen werden", e);
        }
    }

    /** Legt eine neue Vorlage an oder aktualisiert eine bestehende, je nachdem ob id gesetzt ist. */
    public Briefvorlage speichern(Briefvorlage vorlage) {
        return vorlage.getId() == null ? insert(vorlage) : update(vorlage);
    }

    public void loeschen(long id) {
        String sql = "DELETE FROM briefvorlage WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Serienbriefvorlage konnte nicht geloescht werden", e);
        }
    }

    private Briefvorlage insert(Briefvorlage vorlage) {
        String sql = """
                INSERT INTO briefvorlage (name, betreff, dateiname, speicherpfad, brieftext, signatur_name, signatur_rolle)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            binde(ps, vorlage);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                vorlage.setId(keys.getLong(1));
            }
            return vorlage;
        } catch (SQLException e) {
            throw new PersistenceException("Serienbriefvorlage konnte nicht angelegt werden", e);
        }
    }

    private Briefvorlage update(Briefvorlage vorlage) {
        String sql = """
                UPDATE briefvorlage SET name = ?, betreff = ?, dateiname = ?, speicherpfad = ?,
                       brieftext = ?, signatur_name = ?, signatur_rolle = ? WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binde(ps, vorlage);
            ps.setLong(8, vorlage.getId());
            ps.executeUpdate();
            return vorlage;
        } catch (SQLException e) {
            throw new PersistenceException("Serienbriefvorlage konnte nicht aktualisiert werden", e);
        }
    }

    private void binde(PreparedStatement ps, Briefvorlage vorlage) throws SQLException {
        ps.setString(1, vorlage.getName());
        ps.setString(2, vorlage.getBetreff());
        ps.setString(3, vorlage.getDateiname());
        ps.setString(4, vorlage.getSpeicherpfad());
        ps.setString(5, vorlage.getBrieftext());
        ps.setString(6, vorlage.getSignaturName());
        ps.setString(7, vorlage.getSignaturRolle());
    }

    private Briefvorlage map(ResultSet rs) throws SQLException {
        Briefvorlage vorlage = new Briefvorlage();
        vorlage.setId(rs.getLong("id"));
        vorlage.setName(rs.getString("name"));
        vorlage.setBetreff(rs.getString("betreff"));
        vorlage.setDateiname(rs.getString("dateiname"));
        vorlage.setSpeicherpfad(rs.getString("speicherpfad"));
        vorlage.setBrieftext(rs.getString("brieftext"));
        vorlage.setSignaturName(rs.getString("signatur_name"));
        vorlage.setSignaturRolle(rs.getString("signatur_rolle"));
        return vorlage;
    }
}
