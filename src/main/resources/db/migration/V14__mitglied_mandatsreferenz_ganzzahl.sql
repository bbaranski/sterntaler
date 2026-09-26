-- Mandatsreferenz wird von Freitext in eine Ganzzahl umgewandelt. Tabellen-Neubau-Muster (wie in
-- V5__geschaeftsjahr_zeitraum_startsaldo.sql), da SQLite keine direkte Typaenderung einer Spalte
-- unterstuetzt. Nicht-numerische oder leere Altwerte werden zu NULL (keine Mandatsreferenz
-- hinterlegt), statt sie fehlerhaft auf 0 abzubilden.
CREATE TABLE mitglied_neu (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    vorname             TEXT NOT NULL,
    nachname            TEXT NOT NULL,
    strasse             TEXT,
    plz                 TEXT,
    ort                 TEXT,
    eintrittsdatum      TEXT,
    austrittsdatum      TEXT,
    status              TEXT NOT NULL DEFAULT 'AKTIV' CHECK (status IN ('AKTIV', 'INAKTIV')),
    notiz               TEXT,
    email               TEXT,
    iban                TEXT,
    bic                 TEXT,
    jahresbeitrag_cent  INTEGER,
    anrede              TEXT,
    mandatsreferenz     INTEGER,
    sammellastschrift   TEXT
);

INSERT INTO mitglied_neu (id, vorname, nachname, strasse, plz, ort, eintrittsdatum, austrittsdatum,
       status, notiz, email, iban, bic, jahresbeitrag_cent, anrede, mandatsreferenz, sammellastschrift)
SELECT id, vorname, nachname, strasse, plz, ort, eintrittsdatum, austrittsdatum,
       status, notiz, email, iban, bic, jahresbeitrag_cent, anrede,
       CASE WHEN mandatsreferenz IS NOT NULL AND mandatsreferenz GLOB '[0-9]*'
            THEN CAST(mandatsreferenz AS INTEGER)
            ELSE NULL
       END,
       sammellastschrift
FROM mitglied;

DROP TABLE mitglied;
ALTER TABLE mitglied_neu RENAME TO mitglied;
UPDATE sqlite_sequence SET seq = (SELECT MAX(id) FROM mitglied) WHERE name = 'mitglied';
CREATE INDEX idx_mitglied_nachname ON mitglied (nachname, vorname);
