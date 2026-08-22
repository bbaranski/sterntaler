-- Kernschema Phase 1: Konten, Kategorien, Buchungen.
-- Mitglieder, Belege, Geschaeftsjahre und Audit-Log folgen in spaeteren Phasen als eigene Migrationen.
-- Betraege werden als Cent (INTEGER) gespeichert, um Rundungsfehler zu vermeiden.

CREATE TABLE konto (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    bezeichnung       TEXT NOT NULL,
    iban              TEXT NOT NULL UNIQUE,
    bic               TEXT,
    bankname          TEXT,
    saldo_stand_cent  INTEGER,
    saldo_stand_datum TEXT,
    -- HBCI/FinTS-Zugangsdaten: nur nicht-geheime Metadaten. PIN und Passport-Passphrase
    -- liegen ausschliesslich im OS-Schluesselbund (siehe CredentialStore).
    hbci_blz          TEXT,
    hbci_user_id      TEXT,
    hbci_customer_id  TEXT,
    erstellt_am       TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE kategorie (
    id                           INTEGER PRIMARY KEY AUTOINCREMENT,
    name                         TEXT NOT NULL,
    typ                          TEXT NOT NULL CHECK (typ IN ('EINNAHME', 'AUSGABE')),
    uebergeordnete_kategorie_id  INTEGER REFERENCES kategorie (id),
    aktiv                        INTEGER NOT NULL DEFAULT 1,
    UNIQUE (name, typ)
);

CREATE TABLE buchung (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    konto_id            INTEGER NOT NULL REFERENCES konto (id),
    kategorie_id        INTEGER REFERENCES kategorie (id),
    buchungsdatum       TEXT NOT NULL,
    wertstellung        TEXT,
    betrag_cent         INTEGER NOT NULL,
    waehrung            TEXT NOT NULL DEFAULT 'EUR',
    verwendungszweck    TEXT,
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

-- Verhindert Doppel-Import desselben HBCI-Umsatzes je Konto.
CREATE UNIQUE INDEX idx_buchung_hbci_hash ON buchung (konto_id, hbci_import_hash) WHERE hbci_import_hash IS NOT NULL;
CREATE INDEX idx_buchung_datum ON buchung (buchungsdatum);
CREATE INDEX idx_buchung_kategorie ON buchung (kategorie_id);

-- Starter-Kategorien, ueber die UI beliebig erweiterbar.
INSERT INTO kategorie (name, typ) VALUES
    ('Spenden', 'EINNAHME'),
    ('Veranstaltungserloese', 'EINNAHME'),
    ('Zuschuesse', 'EINNAHME'),
    ('Sonstige Einnahmen', 'EINNAHME'),
    ('Vereinsbedarf', 'AUSGABE'),
    ('Veranstaltungskosten', 'AUSGABE'),
    ('Bankgebuehren', 'AUSGABE'),
    ('Versicherung', 'AUSGABE'),
    ('Sonstige Ausgaben', 'AUSGABE');
