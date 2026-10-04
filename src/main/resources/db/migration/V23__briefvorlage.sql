-- Serienbrief-Vorlagen (Word-Dateien mit Platzhaltern) fuer wiederkehrende Mitglieder-Anlaesse
-- (Vorstandssitzung, Jahreshauptversammlung, ...). Die .docx-Datei selbst liegt als Datei im
-- Vorlagen-Verzeichnis neben der .db-Datei (siehe AppConfig.vorlagenVerzeichnis), nicht als BLOB
-- in der Datenbank - gleiches Prinzip wie bei Belegen (siehe V22/BelegDateiMigration).
CREATE TABLE briefvorlage (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL UNIQUE,
    betreff         TEXT NOT NULL,
    dateiname       TEXT NOT NULL,
    speicherpfad    TEXT NOT NULL,
    brieftext       TEXT NOT NULL,
    signatur_name   TEXT,
    signatur_rolle  TEXT
);
