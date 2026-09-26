-- Hochgeladene Belege (Rechnung/Quittung als PDF oder JPG) zu Buchungen. Bewusst als BLOB in
-- der Datenbank selbst statt als Datei auf der Festplatte gespeichert: so bleiben Belege
-- automatisch konsistent mit der jeweils gewaehlten/gesicherten .db-Datei (siehe
-- DatenbankAuswahlDialog und BackupService.sichereDatenbank/VACUUM INTO), ohne zusaetzliche
-- Logik zum Mitkopieren von Dateien. Genau ein Beleg je Buchung (buchung_id UNIQUE) - ein
-- erneuter Upload ersetzt den vorhandenen Beleg, statt einen weiteren anzulegen.
CREATE TABLE beleg (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    buchung_id      INTEGER NOT NULL UNIQUE REFERENCES buchung (id),
    dateiname       TEXT NOT NULL,
    dateityp        TEXT NOT NULL,
    inhalt          BLOB NOT NULL,
    hochgeladen_am  TEXT NOT NULL
);
