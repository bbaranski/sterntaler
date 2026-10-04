package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.foerderverein.kasse.domain.Anrede;
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

    public void loesche(long id) {
        String sql = "DELETE FROM mitglied WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Mitglied konnte nicht geloescht werden", e);
        }
    }

    private Mitglied insert(Mitglied mitglied) {
        String sql = """
                INSERT INTO mitglied (vorname, nachname, strasse, plz, ort, email, eintrittsdatum,
                       austrittsdatum, status, notiz, iban, bic, jahresbeitrag_cent, anrede,
                       mandatsreferenz, sammellastschrift, telefon)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                       eintrittsdatum = ?, austrittsdatum = ?, status = ?, notiz = ?, iban = ?, bic = ?,
                       jahresbeitrag_cent = ?, anrede = ?, mandatsreferenz = ?, sammellastschrift = ?,
                       telefon = ?
                WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, mitglied);
            ps.setLong(18, mitglied.getId());
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
        ps.setString(7, mitglied.getEintrittsdatum());
        ps.setString(8, mitglied.getAustrittsdatum());
        ps.setString(9, mitglied.getStatus().name());
        ps.setString(10, mitglied.getNotiz());
        ps.setString(11, mitglied.getIban());
        ps.setString(12, mitglied.getBic());
        if (mitglied.getJahresbeitragCent() != null) {
            ps.setLong(13, mitglied.getJahresbeitragCent());
        } else {
            ps.setNull(13, java.sql.Types.INTEGER);
        }
        ps.setString(14, mitglied.getAnrede() != null ? mitglied.getAnrede().name() : null);
        if (mitglied.getMandatsreferenz() != null) {
            ps.setLong(15, mitglied.getMandatsreferenz());
        } else {
            ps.setNull(15, java.sql.Types.INTEGER);
        }
        ps.setString(16, mitglied.getSammellastschrift());
        ps.setString(17, mitglied.getTelefon());
    }

    private Mitglied map(ResultSet rs) throws SQLException {
        Mitglied mitglied = new Mitglied();
        mitglied.setId(rs.getLong("id"));
        mitglied.setVorname(rs.getString("vorname"));
        mitglied.setNachname(rs.getString("nachname"));
        mitglied.setStrasse(rs.getString("strasse"));
        mitglied.setPlz(rs.getString("plz"));
        mitglied.setOrt(rs.getString("ort"));
        mitglied.setTelefon(rs.getString("telefon"));
        mitglied.setEmail(rs.getString("email"));
        mitglied.setEintrittsdatum(rs.getString("eintrittsdatum"));
        mitglied.setAustrittsdatum(rs.getString("austrittsdatum"));
        mitglied.setStatus(MitgliedStatus.valueOf(rs.getString("status")));
        mitglied.setNotiz(rs.getString("notiz"));
        mitglied.setIban(rs.getString("iban"));
        mitglied.setBic(rs.getString("bic"));
        long jahresbeitragCent = rs.getLong("jahresbeitrag_cent");
        mitglied.setJahresbeitragCent(rs.wasNull() ? null : jahresbeitragCent);
        String anrede = rs.getString("anrede");
        mitglied.setAnrede(anrede != null ? Anrede.valueOf(anrede) : null);
        long mandatsreferenz = rs.getLong("mandatsreferenz");
        mitglied.setMandatsreferenz(rs.wasNull() ? null : mandatsreferenz);
        mitglied.setSammellastschrift(rs.getString("sammellastschrift"));
        return mitglied;
    }
}
