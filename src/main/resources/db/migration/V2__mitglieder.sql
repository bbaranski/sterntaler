-- Phase 2: Mitgliederverwaltung. Keine Beitrags-/SEPA-Mandat-Felder, da Mitgliedsbeitraege
-- laut Anforderung nicht verwaltet werden - reines Verzeichnis.

CREATE TABLE mitglied (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    vorname         TEXT NOT NULL,
    nachname        TEXT NOT NULL,
    strasse         TEXT,
    plz             TEXT,
    ort             TEXT,
    eintrittsdatum  TEXT,
    austrittsdatum  TEXT,
    status          TEXT NOT NULL DEFAULT 'AKTIV' CHECK (status IN ('AKTIV', 'INAKTIV')),
    notiz           TEXT
);

CREATE INDEX idx_mitglied_nachname ON mitglied (nachname, vorname);

-- Rein informative Verknuepfung (z. B. "Spende von ...", "Auslagenerstattung an ..."),
-- bewusst ohne Soll/Ist-Abgleich oder Beitragslogik dahinter.
ALTER TABLE buchung ADD COLUMN mitglied_id INTEGER REFERENCES mitglied (id);
