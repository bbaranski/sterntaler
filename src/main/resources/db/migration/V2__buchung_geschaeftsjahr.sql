-- Uebersteuerung der Geschaeftsjahr-Zuordnung je Buchung. NULL (Normalfall) bedeutet: wie bisher
-- aus dem Buchungsdatum abgeleitet. Gesetzt bedeutet: diese Zuordnung gewinnt gegen das Datum.
--
-- Hintergrund: Start- und Enddatum eines Geschaeftsjahres sind tagesgenau, und zwei
-- Geschaeftsjahre duerfen sich keinen Tag teilen. Liegen am Stichtag mehrere Buchungen, von denen
-- nur ein Teil ins Jahr gehoert, liesse sich das sonst nicht ausdruecken - ohne das Buchungsdatum
-- zu verfaelschen, das gegenueber dem Kontoauszug korrekt bleiben muss.
ALTER TABLE buchung ADD COLUMN geschaeftsjahr_id INTEGER REFERENCES geschaeftsjahr (id);
