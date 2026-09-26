-- Die Bereich-Klassifizierung (Ideeller Bereich / Vermoegensverwaltung / Zweckbetrieb /
-- Wirtschaftlicher Geschaeftsbetrieb) fuer die jaehrliche "Ergaenzung zur Steuererklaerung"
-- wandert von Kategorie zu Aktivitaet (siehe V11__kategorie_bereich_entfernen.sql): eine
-- Kategorie wie "Spenden" oder "Vereinsbedarf" beschreibt nur die Art der Ein-/Ausgabe,
-- unabhaengig davon in welcher Sphaere sie anfaellt - die Sphaere haengt an der zugeordneten
-- Aktivitaet/Veranstaltung. Default IDEELLER_BEREICH fuer alle bestehenden Aktivitaeten.
ALTER TABLE aktivitaet ADD COLUMN bereich TEXT NOT NULL DEFAULT 'IDEELLER_BEREICH';
