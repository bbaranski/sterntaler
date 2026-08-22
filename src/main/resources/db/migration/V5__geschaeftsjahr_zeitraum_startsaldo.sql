-- Geschaeftsjahr bekommt einen frei waehlbaren Zeitraum statt eines festen Kalenderjahres,
-- plus einen Startsaldo fuer den Kassenbericht. Tabellen-Neubau-Muster statt ALTER TABLE
-- RENAME COLUMN: sonst bliebe die INTEGER-Typaffinitaet der alten "jahr"-Spalte an der neuen
-- Text-Spalte "bezeichnung" haengen (numerische Bezeichnungen wuerden bei ORDER BY vor
-- alphanumerischen einsortiert). Betrifft nur diese Tabelle, keine Fremdschluessel von
-- buchung o.ae. zeigen auf geschaeftsjahr.

CREATE TABLE geschaeftsjahr_neu (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    bezeichnung           TEXT NOT NULL UNIQUE,
    start_datum           TEXT NOT NULL,
    end_datum             TEXT NOT NULL,
    startsaldo_cent       INTEGER NOT NULL DEFAULT 0,
    abgeschlossen         INTEGER NOT NULL DEFAULT 0,
    abschlussdatum        TEXT,
    kassenpruefung_datum  TEXT,
    kassenpruefer         TEXT
);

INSERT INTO geschaeftsjahr_neu (id, bezeichnung, start_datum, end_datum, startsaldo_cent,
       abgeschlossen, abschlussdatum, kassenpruefung_datum, kassenpruefer)
SELECT id, CAST(jahr AS TEXT), start_datum, end_datum, 0,
       abgeschlossen, abschlussdatum, kassenpruefung_datum, kassenpruefer
FROM geschaeftsjahr;

DROP TABLE geschaeftsjahr;
ALTER TABLE geschaeftsjahr_neu RENAME TO geschaeftsjahr;
UPDATE sqlite_sequence SET seq = (SELECT MAX(id) FROM geschaeftsjahr) WHERE name = 'geschaeftsjahr';
