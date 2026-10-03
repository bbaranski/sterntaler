-- Die Zuordnung einer Buchung zu einem Mitglied (seit V2__mitglieder.sql) entfaellt - Buchungen
-- werden nicht mehr mit Mitgliedern verknuepft. Spalte ohne UNIQUE/PK-Beteiligung und ohne Index,
-- daher einfache ALTER TABLE DROP COLUMN (wie schon in V11/V16 fuer aehnliche Faelle).
ALTER TABLE buchung DROP COLUMN mitglied_id;
