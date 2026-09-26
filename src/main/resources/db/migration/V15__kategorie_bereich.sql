-- Die Bereich-Klassifizierung (Ideeller Bereich / Vermoegensverwaltung / Zweckbetrieb /
-- Wirtschaftlicher Geschaeftsbetrieb) wandert von Aktivitaet zurueck zu Kategorie (siehe
-- V16__aktivitaet_bereich_entfernen.sql) - dieses Mal dauerhaft: jede Buchung soll ueber ihre
-- Kategorie einem Bereich zugeordnet sein, Aktivitaeten/Veranstaltungen werden zu reinem
-- optionalem Reporting-Metadatum ohne Einfluss auf die Steuererklaerung. Das bisherige
-- Einnahme/Ausgabe-Feld (typ) entfaellt dabei komplett - Einnahme/Ausgabe ergibt sich bereits
-- ueberall aus dem Vorzeichen von buchung.betrag_cent.
--
-- Tabellen-Neubau-Muster (wie V5/V14), da typ Teil von UNIQUE (name, typ) ist und SQLite
-- DROP COLUMN bei Spalten in einem UNIQUE-Constraint verweigert.
--
-- Alle Kategorien (Starter- wie eigene) werden pauschal auf IDEELLER_BEREICH gesetzt - eine
-- automatische, "richtige" steuerliche Einordnung pro Kategorie kann diese Migration nicht
-- seriös raten (dieselbe bewusste Entscheidung wie schon in V9). Der Kassenwart muss jede
-- Kategorie einmalig ueber den Kategorien-Dialog auf den korrekten Bereich pruefen/setzen.
CREATE TABLE kategorie_neu (
    id                           INTEGER PRIMARY KEY AUTOINCREMENT,
    name                         TEXT NOT NULL,
    bereich                      TEXT NOT NULL DEFAULT 'IDEELLER_BEREICH'
                                  CHECK (bereich IN ('IDEELLER_BEREICH', 'VERMOEGENSVERWALTUNG',
                                                      'ZWECKBETRIEB', 'WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB')),
    uebergeordnete_kategorie_id  INTEGER REFERENCES kategorie (id),
    aktiv                        INTEGER NOT NULL DEFAULT 1,
    UNIQUE (name)
);

INSERT INTO kategorie_neu (id, name, bereich, uebergeordnete_kategorie_id, aktiv)
SELECT id, name, 'IDEELLER_BEREICH', uebergeordnete_kategorie_id, aktiv
FROM kategorie;

DROP TABLE kategorie;
ALTER TABLE kategorie_neu RENAME TO kategorie;
UPDATE sqlite_sequence SET seq = (SELECT MAX(id) FROM kategorie) WHERE name = 'kategorie';
