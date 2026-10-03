-- Vorbereitung fuer die Verschiebung der Beleg-Dateien aus der Datenbank (BLOB) auf die
-- Festplatte (siehe BelegDateiMigration): zunaechst nur die neue Spalte anlegen, die
-- bestehende inhalt-Spalte bleibt bis zum Abschluss der Datenmigration unberuehrt.
ALTER TABLE beleg ADD COLUMN speicherpfad TEXT;
