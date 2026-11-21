package de.foerderverein.kasse.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** Waechter fuer die Schreibweise der als Text gespeicherten Datumsangaben - dieselbe
 * Umwandlung benutzen Mitgliedsdialog, Mitgliedertabelle und Mitgliederexport. */
class DatumstextTest {

    @Test
    void geschriebenWirdImmerVierstellig() {
        assertEquals("01.03.2026", Datumstext.text(LocalDate.of(2026, 3, 1)),
                "Das angezeigte Datum muss TT.MM.JJJJ sein - JavaFX wuerde sonst \"01.03.26\" zeigen");
        assertEquals("31.12.2025", Datumstext.text(LocalDate.of(2025, 12, 31)));
    }

    @Test
    void leeresDatumBleibtLeer() {
        assertEquals("", Datumstext.text(null));
        assertNull(Datumstext.parse(null));
        assertNull(Datumstext.parse("   "));
    }

    @Test
    void gelesenWirdGrosszuegiger() {
        LocalDate erwartet = LocalDate.of(2026, 3, 1);
        assertEquals(erwartet, Datumstext.parse("01.03.2026"));
        assertEquals(erwartet, Datumstext.parse("1.3.2026"), "einstellige Eingabe");
        assertEquals(erwartet, Datumstext.parse("  01.03.2026  "), "Leerzeichen ringsum");
        assertEquals(erwartet, Datumstext.parse("2026-03-01"), "Altbestand im ISO-Format");
    }

    /** Aus dem Mitgliederimport stammen Datumstexte in US-Schreibweise - Excel zeigt sie so an,
     * und der Import uebernimmt den angezeigten Text. Monat vor Tag ist dort die richtige
     * Lesart. */
    @Test
    void usSchreibweiseAusImportenWirdGelesen() {
        assertEquals(LocalDate.of(2026, 9, 26), Datumstext.parse("9/26/26"));
        assertEquals(LocalDate.of(2026, 9, 26), Datumstext.parse("9/26/2026"));
        assertEquals(LocalDate.of(2026, 3, 4), Datumstext.parse("3/4/26"), "Monat vor Tag");
    }

    /** Alles, was nicht zu lesen ist, ergibt null - der Dialog entscheidet dann, ob er den Text
     * stehen laesst (Altbestand) oder das Speichern abbricht. */
    @Test
    void unlesbaresErgibtNull() {
        assertNull(Datumstext.parse("Frühjahr 2019"));
        assertNull(Datumstext.parse("32.13.2026"));
        assertNull(Datumstext.parse("01.03.26"), "zweistellige Jahreszahl ist nicht eindeutig");
    }

    /** Was in der Mitgliedertabelle steht: lesbare Texte vereinheitlicht, alles andere
     * unveraendert - eine Zelle darf nicht leer werden, nur weil das Datum krumm gespeichert
     * ist. */
    @Test
    void anzeigeVereinheitlichtWasLesbarIst() {
        assertEquals("26.09.2026", Datumstext.anzeige("9/26/26"));
        assertEquals("01.03.2026", Datumstext.anzeige("2026-03-01"));
        assertEquals("01.03.2026", Datumstext.anzeige("01.03.2026"), "bereits richtig");
        assertEquals("Frühjahr 2019", Datumstext.anzeige("Frühjahr 2019"));
        assertNull(Datumstext.anzeige(null), "leere Spalte bleibt leer");
    }
}
