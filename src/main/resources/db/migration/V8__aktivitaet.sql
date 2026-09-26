-- Neue Stammdaten-Tabelle: Zuordnung von Buchungen zu einzelnen Veranstaltungen/Aktivitaeten des
-- Wirtschaftlichen Geschaeftsbetriebs (z. B. "Sankt Martin 2026"), fuer die jaehrliche "Ergaenzung
-- zur Steuererklaerung" (Aufschluesselung je Veranstaltung). Bewusst eigenstaendige Tabelle statt
-- Wiederverwendung von Kategorie, da eine Veranstaltung typischerweise sowohl Einnahmen- als auch
-- Ausgabenbuchungen umfasst. Alte Aktivitaeten werden wie Kategorien deaktiviert statt geloescht,
-- damit bestehende Buchungen ihre Zuordnung behalten.
CREATE TABLE aktivitaet (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    name    TEXT NOT NULL UNIQUE,
    notiz   TEXT,
    aktiv   INTEGER NOT NULL DEFAULT 1
);

ALTER TABLE buchung ADD COLUMN aktivitaet_id INTEGER REFERENCES aktivitaet (id);
CREATE INDEX idx_buchung_aktivitaet ON buchung (aktivitaet_id);
