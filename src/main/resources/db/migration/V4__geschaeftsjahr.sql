-- Phase 3: Geschaeftsjahre fuer den Kassenbericht und den jaehrlichen Abschluss-Workflow.

CREATE TABLE geschaeftsjahr (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    jahr                  INTEGER NOT NULL UNIQUE,
    start_datum           TEXT NOT NULL,
    end_datum             TEXT NOT NULL,
    abgeschlossen         INTEGER NOT NULL DEFAULT 0,
    abschlussdatum        TEXT,
    kassenpruefung_datum  TEXT,
    kassenpruefer         TEXT
);
