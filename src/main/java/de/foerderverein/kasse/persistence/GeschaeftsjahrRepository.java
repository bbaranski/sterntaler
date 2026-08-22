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

import de.foerderverein.kasse.domain.Geschaeftsjahr;

public class GeschaeftsjahrRepository {

    private final Database database;

    public GeschaeftsjahrRepository(Database database) {
        this.database = database;
    }

    public List<Geschaeftsjahr> findAll() {
        String sql = "SELECT * FROM geschaeftsjahr ORDER BY start_datum DESC";
        List<Geschaeftsjahr> result = new ArrayList<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Geschaeftsjahre konnten nicht geladen werden", e);
        }
    }

    /** Liefert das (hoechstens eine) abgeschlossene Geschaeftsjahr, in dessen Zeitraum das
     * gegebene Datum faellt - genutzt, um Buchungen in gesperrten Jahren zu verhindern. */
    public Optional<Geschaeftsjahr> findAbgeschlossenesJahrFuer(LocalDate datum) {
        String sql = "SELECT * FROM geschaeftsjahr WHERE abgeschlossen = 1 AND ? BETWEEN start_datum AND end_datum";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, datum.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Pruefung auf gesperrtes Geschaeftsjahr fehlgeschlagen", e);
        }
    }

    public Geschaeftsjahr save(Geschaeftsjahr geschaeftsjahr) {
        return geschaeftsjahr.getId() == null ? insert(geschaeftsjahr) : update(geschaeftsjahr);
    }

    private Geschaeftsjahr insert(Geschaeftsjahr geschaeftsjahr) {
        String sql = """
                INSERT INTO geschaeftsjahr (bezeichnung, start_datum, end_datum, startsaldo_cent,
                       abgeschlossen, abschlussdatum, kassenpruefung_datum, kassenpruefer)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, geschaeftsjahr);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                geschaeftsjahr.setId(keys.getLong(1));
            }
            return geschaeftsjahr;
        } catch (SQLException e) {
            throw new PersistenceException("Geschaeftsjahr konnte nicht angelegt werden", e);
        }
    }

    private Geschaeftsjahr update(Geschaeftsjahr geschaeftsjahr) {
        String sql = """
                UPDATE geschaeftsjahr SET bezeichnung = ?, start_datum = ?, end_datum = ?, startsaldo_cent = ?,
                       abgeschlossen = ?, abschlussdatum = ?, kassenpruefung_datum = ?, kassenpruefer = ?
                WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, geschaeftsjahr);
            ps.setLong(9, geschaeftsjahr.getId());
            ps.executeUpdate();
            return geschaeftsjahr;
        } catch (SQLException e) {
            throw new PersistenceException("Geschaeftsjahr konnte nicht aktualisiert werden", e);
        }
    }

    private void bind(PreparedStatement ps, Geschaeftsjahr geschaeftsjahr) throws SQLException {
        ps.setString(1, geschaeftsjahr.getBezeichnung());
        ps.setString(2, geschaeftsjahr.getStartDatum().toString());
        ps.setString(3, geschaeftsjahr.getEndDatum().toString());
        ps.setLong(4, geschaeftsjahr.getStartsaldoCent());
        ps.setInt(5, geschaeftsjahr.isAbgeschlossen() ? 1 : 0);
        ps.setString(6, geschaeftsjahr.getAbschlussdatum() != null ? geschaeftsjahr.getAbschlussdatum().toString() : null);
        ps.setString(7, geschaeftsjahr.getKassenpruefungDatum() != null ? geschaeftsjahr.getKassenpruefungDatum().toString() : null);
        ps.setString(8, geschaeftsjahr.getKassenpruefer());
    }

    private Geschaeftsjahr map(ResultSet rs) throws SQLException {
        Geschaeftsjahr geschaeftsjahr = new Geschaeftsjahr();
        geschaeftsjahr.setId(rs.getLong("id"));
        geschaeftsjahr.setBezeichnung(rs.getString("bezeichnung"));
        geschaeftsjahr.setStartDatum(LocalDate.parse(rs.getString("start_datum")));
        geschaeftsjahr.setEndDatum(LocalDate.parse(rs.getString("end_datum")));
        geschaeftsjahr.setStartsaldoCent(rs.getLong("startsaldo_cent"));
        geschaeftsjahr.setAbgeschlossen(rs.getInt("abgeschlossen") == 1);
        String abschlussdatum = rs.getString("abschlussdatum");
        geschaeftsjahr.setAbschlussdatum(abschlussdatum != null ? LocalDate.parse(abschlussdatum) : null);
        String kassenpruefungDatum = rs.getString("kassenpruefung_datum");
        geschaeftsjahr.setKassenpruefungDatum(kassenpruefungDatum != null ? LocalDate.parse(kassenpruefungDatum) : null);
        geschaeftsjahr.setKassenpruefer(rs.getString("kassenpruefer"));
        return geschaeftsjahr;
    }
}
