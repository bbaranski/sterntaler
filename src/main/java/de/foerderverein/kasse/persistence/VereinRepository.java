package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

import de.foerderverein.kasse.domain.Verein;

/** Es gibt stets hoechstens einen Verein-Datensatz (siehe V19__verein.sql) - find() liefert daher
 * einfach die erste (einzige) Zeile statt einer Liste. */
public class VereinRepository {

    private final Database database;

    public VereinRepository(Database database) {
        this.database = database;
    }

    public Optional<Verein> find() {
        String sql = "SELECT * FROM verein ORDER BY id LIMIT 1";
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        } catch (SQLException e) {
            throw new PersistenceException("Vereinsdaten konnten nicht geladen werden", e);
        }
    }

    public Verein save(Verein verein) {
        return verein.getId() == null ? insert(verein) : update(verein);
    }

    private Verein insert(Verein verein) {
        String sql = """
                INSERT INTO verein (name, strasse, plz, ort, telefon, email, homepage, iban, bic, bankname)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, verein);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                verein.setId(keys.getLong(1));
            }
            return verein;
        } catch (SQLException e) {
            throw new PersistenceException("Vereinsdaten konnten nicht angelegt werden", e);
        }
    }

    private Verein update(Verein verein) {
        String sql = """
                UPDATE verein SET name = ?, strasse = ?, plz = ?, ort = ?, telefon = ?, email = ?,
                       homepage = ?, iban = ?, bic = ?, bankname = ?
                WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, verein);
            ps.setLong(11, verein.getId());
            ps.executeUpdate();
            return verein;
        } catch (SQLException e) {
            throw new PersistenceException("Vereinsdaten konnten nicht aktualisiert werden", e);
        }
    }

    private void bind(PreparedStatement ps, Verein verein) throws SQLException {
        ps.setString(1, verein.getName());
        ps.setString(2, verein.getStrasse());
        ps.setString(3, verein.getPlz());
        ps.setString(4, verein.getOrt());
        ps.setString(5, verein.getTelefon());
        ps.setString(6, verein.getEmail());
        ps.setString(7, verein.getHomepage());
        ps.setString(8, verein.getIban());
        ps.setString(9, verein.getBic());
        ps.setString(10, verein.getBankname());
    }

    private Verein map(ResultSet rs) throws SQLException {
        Verein verein = new Verein();
        verein.setId(rs.getLong("id"));
        verein.setName(rs.getString("name"));
        verein.setStrasse(rs.getString("strasse"));
        verein.setPlz(rs.getString("plz"));
        verein.setOrt(rs.getString("ort"));
        verein.setTelefon(rs.getString("telefon"));
        verein.setEmail(rs.getString("email"));
        verein.setHomepage(rs.getString("homepage"));
        verein.setIban(rs.getString("iban"));
        verein.setBic(rs.getString("bic"));
        verein.setBankname(rs.getString("bankname"));
        return verein;
    }
}
