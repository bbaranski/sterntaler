-- Siehe V10__aktivitaet_bereich.sql: die Bereich-Klassifizierung liegt jetzt an der Aktivitaet,
-- nicht mehr an der Kategorie.
ALTER TABLE kategorie DROP COLUMN bereich;
