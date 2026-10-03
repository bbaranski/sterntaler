-- Neue Freitextfelder an der Buchung: Kontoauszug (Referenz auf den Papier-/PDF-Kontoauszug, auf
-- dem dieser Umsatz erscheint) und Rechnungsnummer (Referenz auf den zugehoerigen Beleg/Rechnung).
ALTER TABLE buchung ADD COLUMN kontoauszug TEXT;
ALTER TABLE buchung ADD COLUMN rechnungsnummer TEXT;
