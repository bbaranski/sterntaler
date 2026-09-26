-- Ersetzt die generischen Start-Kategorien aus V1__init.sql durch eine an die Vereinspraxis eines
-- Foerdervereins angepasste Liste, direkt dem passenden steuerlichen Bereich zugeordnet.
-- Verknuepfte Buchungen verlieren dabei ihre Kategorie-Zuordnung (fallen im Steuerreport
-- automatisch in den Ideeller Bereich) statt das Loeschen zu blockieren oder Daten zu verlieren.
UPDATE buchung SET kategorie_id = NULL
WHERE kategorie_id IN (
    SELECT id FROM kategorie WHERE name IN (
        'Spenden', 'Veranstaltungserloese', 'Zuschuesse', 'Sonstige Einnahmen',
        'Vereinsbedarf', 'Veranstaltungskosten', 'Bankgebuehren', 'Versicherung', 'Sonstige Ausgaben'
    )
);

DELETE FROM kategorie WHERE name IN (
    'Spenden', 'Veranstaltungserloese', 'Zuschuesse', 'Sonstige Einnahmen',
    'Vereinsbedarf', 'Veranstaltungskosten', 'Bankgebuehren', 'Versicherung', 'Sonstige Ausgaben'
);

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
