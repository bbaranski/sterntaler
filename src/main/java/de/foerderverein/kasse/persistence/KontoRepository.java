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

    public Konto save(Konto konto) {
        return konto.getId() == null ? insert(konto) : update(konto);
    }

    private Konto insert(Konto konto) {
        String sql = """
                INSERT INTO konto (bezeichnung, iban, bic, bankname, saldo_stand_datum,
                       hbci_blz, hbci_user_id, hbci_customer_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
                       saldo_stand_datum = ?,
                       hbci_blz = ?, hbci_user_id = ?, hbci_customer_id = ?
                WHERE id = ?
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, konto);
            ps.setLong(9, konto.getId());
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
        ps.setString(5, konto.getSaldoStandDatum() != null ? konto.getSaldoStandDatum().toString() : null);
        ps.setString(6, konto.getHbciBlz());
        ps.setString(7, konto.getHbciUserId());
        ps.setString(8, konto.getHbciCustomerId());
    }

    private Konto map(ResultSet rs) throws SQLException {
        Konto konto = new Konto();
        konto.setId(rs.getLong("id"));
        konto.setBezeichnung(rs.getString("bezeichnung"));
        konto.setIban(rs.getString("iban"));
        konto.setBic(rs.getString("bic"));
        konto.setBankname(rs.getString("bankname"));
        String saldoDatum = rs.getString("saldo_stand_datum");
        konto.setSaldoStandDatum(saldoDatum != null ? LocalDate.parse(saldoDatum) : null);
        konto.setHbciBlz(rs.getString("hbci_blz"));
        konto.setHbciUserId(rs.getString("hbci_user_id"));
        konto.setHbciCustomerId(rs.getString("hbci_customer_id"));
        return konto;
    }
}
