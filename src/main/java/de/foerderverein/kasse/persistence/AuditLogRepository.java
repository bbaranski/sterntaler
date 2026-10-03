package de.foerderverein.kasse.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import de.foerderverein.kasse.domain.AuditAktion;
import de.foerderverein.kasse.domain.AuditEintrag;

public class AuditLogRepository {

    private final Database database;

    public AuditLogRepository(Database database) {
        this.database = database;
    }

    public AuditEintrag insert(AuditEintrag eintrag) {
        String sql = """
                INSERT INTO audit_log (entity, entity_id, aktion, details, feld, wert_alt, wert_neu, benutzer, zeitstempel)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, eintrag.getEntity());
            ps.setLong(2, eintrag.getEntityId());
            ps.setString(3, eintrag.getAktion().name());
            ps.setString(4, eintrag.getDetails());
            ps.setString(5, eintrag.getFeld());
            ps.setString(6, eintrag.getWertAlt());
            ps.setString(7, eintrag.getWertNeu());
            ps.setString(8, eintrag.getBenutzer());
            ps.setString(9, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                eintrag.setId(keys.getLong(1));
            }
            return eintrag;
        } catch (SQLException e) {
            throw new PersistenceException("Audit-Eintrag konnte nicht gespeichert werden", e);
        }
    }

    public List<AuditEintrag> findByEntity(String entity, long entityId) {
        String sql = "SELECT * FROM audit_log WHERE entity = ? AND entity_id = ? ORDER BY zeitstempel DESC, id DESC";
        List<AuditEintrag> result = new ArrayList<>();
        try (Connection c = database.connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entity);
            ps.setLong(2, entityId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new PersistenceException("Audit-Eintraege konnten nicht geladen werden", e);
        }
    }

    private AuditEintrag map(ResultSet rs) throws SQLException {
        AuditEintrag eintrag = new AuditEintrag();
        eintrag.setId(rs.getLong("id"));
        eintrag.setEntity(rs.getString("entity"));
        eintrag.setEntityId(rs.getLong("entity_id"));
        eintrag.setAktion(AuditAktion.valueOf(rs.getString("aktion")));
        eintrag.setDetails(rs.getString("details"));
        eintrag.setFeld(rs.getString("feld"));
        eintrag.setWertAlt(rs.getString("wert_alt"));
        eintrag.setWertNeu(rs.getString("wert_neu"));
        eintrag.setBenutzer(rs.getString("benutzer"));
        eintrag.setZeitstempel(LocalDateTime.parse(rs.getString("zeitstempel")));
        return eintrag;
    }
}
