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

import de.foerderverein.kasse.domain.Konto;

public class KontoRepository {

    private final Database database;

    public KontoRepository(Database database) {
        this.database = database;
    }

    public List<Konto> findAll() {
        String sql = "SELECT * FROM konto ORDER BY bezeichnung";
        List<Konto> result = new ArrayList<>();
        try (Connection c = database.connection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                result.add(map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Konten konnten nicht geladen werden", e);
        }
    }

    public Optional<Konto> findById(long id) {
        String sql = "SELECT * FROM konto WHERE id = ?";
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Konto konnte nicht geladen werden", e);
        }
    }

    public Konto save(Konto konto) {
        return konto.getId() == null ? insert(konto) : update(konto);
    }

    private Konto insert(Konto konto) {
        String sql = """
                INSERT INTO konto (bezeichnung, iban, bic, bankname, saldo_stand_cent, saldo_stand_datum,
                       hbci_blz, hbci_user_id, hbci_customer_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, konto);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                konto.setId(keys.getLong(1));
            }
            return konto;
        } catch (SQLException e) {
            throw new PersistenceException("Konto konnte nicht angelegt werden", e);
        }
    }

    private Konto update(Konto konto) {
        String sql = """
                UPDATE konto SET bezeichnung = ?, iban = ?, bic = ?, bankname = ?,
                       saldo_stand_cent = ?, saldo_stand_datum = ?,
                       hbci_blz = ?, hbci_user_id = ?, hbci_customer_id = ?
                WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, konto);
            ps.setLong(10, konto.getId());
            ps.executeUpdate();
            return konto;
        } catch (SQLException e) {
            throw new PersistenceException("Konto konnte nicht aktualisiert werden", e);
        }
    }

    private void bind(PreparedStatement ps, Konto konto) throws SQLException {
        ps.setString(1, konto.getBezeichnung());
        ps.setString(2, konto.getIban());
        ps.setString(3, konto.getBic());
        ps.setString(4, konto.getBankname());
        if (konto.getSaldoStandCent() != null) {
            ps.setLong(5, konto.getSaldoStandCent());
        } else {
            ps.setNull(5, java.sql.Types.INTEGER);
        }
        ps.setString(6, konto.getSaldoStandDatum() != null ? konto.getSaldoStandDatum().toString() : null);
        ps.setString(7, konto.getHbciBlz());
        ps.setString(8, konto.getHbciUserId());
        ps.setString(9, konto.getHbciCustomerId());
    }

    private Konto map(ResultSet rs) throws SQLException {
        Konto konto = new Konto();
        konto.setId(rs.getLong("id"));
        konto.setBezeichnung(rs.getString("bezeichnung"));
        konto.setIban(rs.getString("iban"));
        konto.setBic(rs.getString("bic"));
        konto.setBankname(rs.getString("bankname"));
        long saldoCent = rs.getLong("saldo_stand_cent");
        konto.setSaldoStandCent(rs.wasNull() ? null : saldoCent);
        String saldoDatum = rs.getString("saldo_stand_datum");
        konto.setSaldoStandDatum(saldoDatum != null ? LocalDate.parse(saldoDatum) : null);
        konto.setHbciBlz(rs.getString("hbci_blz"));
        konto.setHbciUserId(rs.getString("hbci_user_id"));
        konto.setHbciCustomerId(rs.getString("hbci_customer_id"));
        return konto;
    }
}
