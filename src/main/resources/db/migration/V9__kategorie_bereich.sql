-- Bereich-Klassifizierung (Ideeller Bereich / Vermoegensverwaltung / Zweckbetrieb /
-- Wirtschaftlicher Geschaeftsbetrieb) je Kategorie, fuer die jaehrliche "Ergaenzung zur
-- Steuererklaerung". Default IDEELLER_BEREICH fuer alle bestehenden Kategorien - bewusst keine
-- automatische Zuordnung der Veranstaltungs-Kategorien ("Veranstaltungserloese"/
-- "Veranstaltungskosten") zu WIRTSCHAFTLICHER_GESCHAEFTSBETRIEB; das entscheidet der Kassenwart
-- manuell je Kategorie ueber den Kategorien-Dialog.
ALTER TABLE kategorie ADD COLUMN bereich TEXT NOT NULL DEFAULT 'IDEELLER_BEREICH';
