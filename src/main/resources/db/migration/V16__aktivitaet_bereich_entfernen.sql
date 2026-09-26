-- Siehe V15__kategorie_bereich.sql: die Bereich-Klassifizierung liegt jetzt dauerhaft an der
-- Kategorie, nicht mehr an der Aktivitaet. Aktivitaet ist damit reines optionales
-- Reporting-Metadatum ohne Einfluss auf die Steuererklaerung.
ALTER TABLE aktivitaet DROP COLUMN bereich;
