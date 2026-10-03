-- Ergaenzt den Audit-Log um eine feldbasierte Aenderungsdarstellung ("Feld"/"Wert (alt)"/
-- "Wert (neu)") fuer GEAENDERT-Eintraege, damit im Verlauf-Dialog erkennbar ist, was sich konkret
-- geaendert hat, statt nur einer zusammenfassenden Textbeschreibung (details). Bestehende
-- Eintraege (ANGELEGT/STORNIERT/GELOESCHT) nutzen weiterhin details und lassen diese drei neuen
-- Spalten leer.
ALTER TABLE audit_log ADD COLUMN feld TEXT;
ALTER TABLE audit_log ADD COLUMN wert_alt TEXT;
ALTER TABLE audit_log ADD COLUMN wert_neu TEXT;
