-- Initialschema der Vereinskasse (Stand Version 1.0).
--
-- Diese Datei fasst die urspruenglich 24 einzelnen Migrationsschritte zusammen, die waehrend der
-- Entwicklung entstanden sind. Sie beschreibt das Schema vollstaendig und ist die einzige Quelle
-- dafuer - fruehere Versionen verteilten es auf Flyway-Skripte plus einen nachgelagerten
-- Java-Schritt (Beleg-BLOBs -> Dateien), was bei einer Neuinstallation zu einer nicht
-- befuellbaren Spalte "beleg.inhalt" fuehrte.
--
-- Bestehende Datenbanken aelterer Versionen lassen sich damit bewusst nicht mehr oeffnen:
-- Flyway bricht dann mit einem Validierungsfehler ab, statt ein inkompatibles Altschema
-- stillschweigend weiterzuverwenden.

CREATE TABLE konto (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    bezeichnung       TEXT NOT NULL,
    iban              TEXT NOT NULL UNIQUE,
    bic               TEXT,
    bankname          TEXT,
    saldo_stand_datum TEXT,
    -- HBCI/FinTS-Zugangsdaten: nur nicht-geheime Metadaten. PIN und Passport-Passphrase
    -- liegen ausschliesslich im OS-Schluesselbund (siehe CredentialStore).
    hbci_blz          TEXT,
    hbci_user_id      TEXT,
    hbci_customer_id  TEXT,
    erstellt_am       TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Der steuerlich relevante Bereich haengt an der Kategorie selbst; eine optional zugeordnete
-- Aktivitaet ist reines Reporting-Metadatum ohne Einfluss auf die Steuererklaerung.
CREATE TABLE kategorie (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    name     TEXT NOT NULL,
    bereich  TEXT NOT NULL DEFAULT 'IDEELLER_BEREICH'
              CHECK (bereich IN ('IDEELLER_BEREICH', 'VERMOEGENSVERWALTUNG',
                                 'ZWECKBETRIEB', 'WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB')),
    aktiv    INTEGER NOT NULL DEFAULT 1,
    UNIQUE (name)
);

CREATE TABLE aktivitaet (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name    TEXT NOT NULL UNIQUE,
    notiz   TEXT,
    aktiv   INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE buchung (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    konto_id            INTEGER NOT NULL REFERENCES konto (id),
    kategorie_id        INTEGER REFERENCES kategorie (id),
    aktivitaet_id       INTEGER REFERENCES aktivitaet (id),
    buchungsdatum       TEXT NOT NULL,
    wertstellung        TEXT,
    betrag_cent         INTEGER NOT NULL,
    kontoauszug         TEXT,
    waehrung            TEXT NOT NULL DEFAULT 'EUR',
    verwendungszweck    TEXT,
    rechnungsnummer     TEXT,
    gegenkonto_iban     TEXT,
    gegenkonto_name     TEXT,
    quelle              TEXT NOT NULL CHECK (quelle IN ('MANUELL', 'HBCI_IMPORT')),
    hbci_import_hash    TEXT,
    storniert           INTEGER NOT NULL DEFAULT 0,
    storno_referenz_id  INTEGER REFERENCES buchung (id),
    erstellt_von        TEXT,
    erstellt_am         TEXT NOT NULL DEFAULT (datetime('now')),
    geaendert_am        TEXT
);

CREATE TABLE mitglied (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    vorname             TEXT NOT NULL,
    nachname            TEXT NOT NULL,
    strasse             TEXT,
    plz                 TEXT,
    ort                 TEXT,
    telefon             TEXT,
    email               TEXT,
    eintrittsdatum      TEXT,
    austrittsdatum      TEXT,
    status              TEXT NOT NULL DEFAULT 'AKTIV' CHECK (status IN ('AKTIV', 'INAKTIV')),
    notiz               TEXT,
    iban                TEXT,
    bic                 TEXT,
    jahresbeitrag_cent  INTEGER,
    anrede              TEXT,
    mandatsreferenz     INTEGER,
    sammellastschrift   TEXT
);

CREATE TABLE geschaeftsjahr (
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

CREATE TABLE audit_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    entity       TEXT NOT NULL,
    entity_id    INTEGER NOT NULL,
    aktion       TEXT NOT NULL,
    details      TEXT,
    feld         TEXT,
    wert_alt     TEXT,
    wert_neu     TEXT,
    benutzer     TEXT,
    zeitstempel  TEXT NOT NULL DEFAULT (datetime('now'))
);

-- Genau ein Beleg je Buchung (buchung_id UNIQUE) - ein erneuter Upload ersetzt den vorhandenen.
-- Die Datei selbst liegt im Belege-Verzeichnis neben der .db-Datei (siehe
-- AppConfig.belegeVerzeichnis); speicherpfad ist nur der Dateiname darin.
CREATE TABLE beleg (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    buchung_id      INTEGER NOT NULL UNIQUE REFERENCES buchung (id),
    dateiname       TEXT NOT NULL,
    dateityp        TEXT NOT NULL,
    speicherpfad    TEXT,
    hochgeladen_am  TEXT NOT NULL
);

-- Stammdaten des Vereins selbst - genau ein Datensatz, fuer den Briefkopf der Berichte.
CREATE TABLE verein (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    name      TEXT NOT NULL,
    strasse   TEXT,
    plz       TEXT,
    ort       TEXT,
    telefon   TEXT,
    email     TEXT,
    homepage  TEXT,
    iban      TEXT,
    bic       TEXT,
    bankname  TEXT
);

-- Serienbrief-Vorlagen; die .docx-Datei liegt im Vorlagen-Verzeichnis neben der .db-Datei
-- (siehe AppConfig.vorlagenVerzeichnis).
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

CREATE UNIQUE INDEX idx_buchung_hbci_hash ON buchung (konto_id, hbci_import_hash) WHERE hbci_import_hash IS NOT NULL;
CREATE INDEX idx_buchung_datum ON buchung (buchungsdatum);
CREATE INDEX idx_buchung_kategorie ON buchung (kategorie_id);
CREATE INDEX idx_buchung_aktivitaet ON buchung (aktivitaet_id);
CREATE INDEX idx_mitglied_nachname ON mitglied (nachname, vorname);
CREATE INDEX idx_audit_log_entity ON audit_log (entity, entity_id);

-- Foerderverein-spezifische Standardkategorien. Kategorien unterscheiden bewusst nicht zwischen
-- Einnahme und Ausgabe - das ergibt sich bereits aus dem Vorzeichen der Buchung.
INSERT INTO kategorie (name, bereich, aktiv) VALUES
    ('Mitgliedsbeiträge', 'IDEELLER_BEREICH', 1),
    ('Geldspenden', 'IDEELLER_BEREICH', 1),
    ('Klassenfahrten und Ausflüge', 'IDEELLER_BEREICH', 1),
    ('Anschaffungen für die Schule', 'IDEELLER_BEREICH', 1),
    ('AGs, Projekte und Workshops', 'IDEELLER_BEREICH', 1),
    ('Schulfeste und Veranstaltungen', 'IDEELLER_BEREICH', 1),
    ('Einschulung und Abschiede', 'IDEELLER_BEREICH', 1),
    ('Schulbücherei, Pausenhof und Garten', 'IDEELLER_BEREICH', 1),
    ('Kontoführung und Bankgebühren', 'IDEELLER_BEREICH', 1),
    ('Versicherungen', 'IDEELLER_BEREICH', 1),
    ('Porto, Druck und Büromaterial', 'IDEELLER_BEREICH', 1),
    ('Website, Domain und Software', 'IDEELLER_BEREICH', 1),
    ('Mitgliedschaften', 'IDEELLER_BEREICH', 1),
    ('Vereinsregister und Notarkosten', 'IDEELLER_BEREICH', 1),
    ('Zinserträge', 'VERMOEGENSVERWALTUNG', 1),
    ('Veranstaltungen (Zweckbetrieb)', 'ZWECKBETRIEB', 1),
    ('Kuchen- und Getränkeverkauf', 'WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB', 1),
    ('Verkauf von T-Shirts und Jahrbüchern', 'WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB', 1),
    ('Flohmarkt- und Basareinnahmen', 'WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB', 1),
    ('Sponsoring und Anzeigen', 'WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB', 1),
    ('Wareneinkauf für Verkäufe', 'WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB', 1),
    ('Standgebühren und Material', 'WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB', 1);
