-- Stammdaten des Vereins selbst (Name, Adresse, Kontakt, Bankverbindung) - fuer den Briefkopf der
-- erzeugten Berichte (Kassenbericht, Ergaenzung zur Steuererklaerung). Es gibt bewusst keine
-- Mehrfachauswahl wie bei Konto/Kategorie: die Anwendung verwaltet genau einen Verein, daher kein
-- UNIQUE/CHECK-Zwang auf der Datenbankseite - die Anwendungsschicht (VereinRepository) liest/
-- schreibt immer nur die eine vorhandene Zeile.
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
