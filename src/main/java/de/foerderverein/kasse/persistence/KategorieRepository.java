package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import de.foerderverein.kasse.domain.Bereich;
import de.foerderverein.kasse.domain.Kategorie;

public class KategorieRepository {

    // Sortiert nach der fachlich korrekten, deklarierten Reihenfolge des Bereich-Enums (Ideeller
    // Bereich, Vermoegensverwaltung, Zweckbetrieb, Wirtschaftlicher Geschaeftsbetrieb) statt nach
    // dem alphabetischen Enum-Text (was z. B. "Wirtschaftlicher Geschaeftsbetrieb" faelschlich vor
    // "Zweckbetrieb" einsortieren wuerde) - deshalb Java-seitig statt per SQL ORDER BY.
    private static final Comparator<Kategorie> SORTIERUNG =
            Comparator.comparing(Kategorie::getBereich).thenComparing(Kategorie::getName);

    private final Database database;

    public KategorieRepository(Database database) {
        this.database = database;
    }

    public List<Kategorie> findAllAktiv() {
        String sql = "SELECT * FROM kategorie WHERE aktiv = 1";
        return query(sql);
    }

    public List<Kategorie> findAll() {
        String sql = "SELECT * FROM kategorie";
        return query(sql);
    }

    private List<Kategorie> query(String sql) {
        List<Kategorie> result = new ArrayList<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result.stream().sorted(SORTIERUNG).toList();
        } catch (SQLException e) {
            throw new PersistenceException("Kategorien konnten nicht geladen werden", e);
        }
    }

    public Optional<Kategorie> findById(long id) {
        String sql = "SELECT * FROM kategorie WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Kategorie konnte nicht geladen werden", e);
        }
    }

    public Kategorie save(Kategorie kategorie) {
        return kategorie.getId() == null ? insert(kategorie) : update(kategorie);
    }

    private Kategorie insert(Kategorie kategorie) {
        String sql = """
                INSERT INTO kategorie (name, bereich, uebergeordnete_kategorie_id, aktiv)
                VALUES (?, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, kategorie);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                kategorie.setId(keys.getLong(1));
            }
            return kategorie;
        } catch (SQLException e) {
            throw new PersistenceException("Kategorie konnte nicht angelegt werden", e);
        }
    }

    private Kategorie update(Kategorie kategorie) {
        String sql = """
                UPDATE kategorie SET name = ?, bereich = ?, uebergeordnete_kategorie_id = ?, aktiv = ?
                WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, kategorie);
            ps.setLong(5, kategorie.getId());
            ps.executeUpdate();
            return kategorie;
        } catch (SQLException e) {
            throw new PersistenceException("Kategorie konnte nicht aktualisiert werden", e);
        }
    }

    private void bind(PreparedStatement ps, Kategorie kategorie) throws SQLException {
        ps.setString(1, kategorie.getName());
        ps.setString(2, kategorie.getBereich().name());
        if (kategorie.getUebergeordneteKategorieId() != null) {
            ps.setLong(3, kategorie.getUebergeordneteKategorieId());
        } else {
            ps.setNull(3, java.sql.Types.INTEGER);
        }
        ps.setInt(4, kategorie.isAktiv() ? 1 : 0);
    }

    private Kategorie map(ResultSet rs) throws SQLException {
        Kategorie kategorie = new Kategorie();
        kategorie.setId(rs.getLong("id"));
        kategorie.setName(rs.getString("name"));
        kategorie.setBereich(Bereich.valueOf(rs.getString("bereich")));
        long parent = rs.getLong("uebergeordnete_kategorie_id");
        kategorie.setUebergeordneteKategorieId(rs.wasNull() ? null : parent);
        kategorie.setAktiv(rs.getInt("aktiv") == 1);
        return kategorie;
    }
}
