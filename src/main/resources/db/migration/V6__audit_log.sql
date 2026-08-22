-- Phase 4: Audit-Log fuer Buchungen - Nachvollziehbarkeit fuer Kassenpruefer und beim
-- Wechsel zum Nachfolger (wer hat wann welche Buchung angelegt/geaendert/storniert).

CREATE TABLE audit_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    entity       TEXT NOT NULL,
    entity_id    INTEGER NOT NULL,
    aktion       TEXT NOT NULL,
    details      TEXT,
    benutzer     TEXT,
    zeitstempel  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity, entity_id);
