-- Zusaetzliche Mitgliederfelder: Anrede (fuer Briefe/Serienbriefe) sowie Mandatsreferenz und
-- Sammellastschrift (SEPA-Lastschrift-Referenzdaten, rein informativ - wie IBAN/BIC keine
-- automatisierte Lastschrifterstellung, siehe V7__mitglied_bankdaten.sql).
ALTER TABLE mitglied ADD COLUMN anrede TEXT;
ALTER TABLE mitglied ADD COLUMN mandatsreferenz TEXT;
ALTER TABLE mitglied ADD COLUMN sammellastschrift TEXT;
