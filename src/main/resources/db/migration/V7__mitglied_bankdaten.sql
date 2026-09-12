-- Rein informative Referenzfelder (kein SEPA-Mandat, keine automatisierte Lastschrift/Beitragsverwaltung -
-- siehe V2__mitglieder.sql): IBAN/BIC fuer manuellen Abgleich mit HBCI-Umsaetzen, Jahresbeitrag als
-- Referenzwert fuer den Kassenwart.
ALTER TABLE mitglied ADD COLUMN iban TEXT;
ALTER TABLE mitglied ADD COLUMN bic TEXT;
ALTER TABLE mitglied ADD COLUMN jahresbeitrag_cent INTEGER;
