package de.foerderverein.kasse.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BuchungTest {

    @Test
    void ausgabeMitGegenkontoOhneRechnungsnummerFehlt() {
        assertTrue(ausgabeMitGegenkonto().fehlendeRechnungsnummer());
    }

    @Test
    void ausgabeMitGegenkontoUndRechnungsnummerIstVollstaendig() {
        Buchung b = ausgabeMitGegenkonto();
        b.setRechnungsnummer("2026-017");

        assertFalse(b.fehlendeRechnungsnummer());
    }

    /** Leerzeichen sind keine Angabe - sonst liesse sich die Markierung durch ein Blank umgehen. */
    @Test
    void nurLeerzeichenGiltNichtAlsRechnungsnummer() {
        Buchung b = ausgabeMitGegenkonto();
        b.setRechnungsnummer("   ");

        assertTrue(b.fehlendeRechnungsnummer());
    }

    @Test
    void einnahmeWirdNichtGeprueft() {
        Buchung b = ausgabeMitGegenkonto();
        b.setBetragEuro(new BigDecimal("25.00"));

        assertFalse(b.fehlendeRechnungsnummer());
    }

    @Test
    void betragNullWirdNichtGeprueft() {
        Buchung b = ausgabeMitGegenkonto();
        b.setBetragCent(0);

        assertFalse(b.fehlendeRechnungsnummer());
    }

    @Test
    void ausgabeOhneGegenkontoWirdNichtGeprueft() {
        Buchung b = ausgabeMitGegenkonto();
        b.setGegenkontoName(null);
        b.setGegenkontoIban(null);

        assertFalse(b.fehlendeRechnungsnummer());
    }

    /** Beim Import liefert die Bank nicht immer beide Felder - eines genuegt als Gegenkonto. */
    @Test
    void alleinDieIbanGenuegtAlsGegenkonto() {
        Buchung b = ausgabeMitGegenkonto();
        b.setGegenkontoName(null);

        assertTrue(b.fehlendeRechnungsnummer());
    }

    @Test
    void alleinDerNameGenuegtAlsGegenkonto() {
        Buchung b = ausgabeMitGegenkonto();
        b.setGegenkontoIban(null);

        assertTrue(b.fehlendeRechnungsnummer());
    }

    @Test
    void leeresGegenkontoFeldGiltNichtAlsGegenkonto() {
        Buchung b = ausgabeMitGegenkonto();
        b.setGegenkontoName("  ");
        b.setGegenkontoIban("");

        assertFalse(b.fehlendeRechnungsnummer());
    }

    /** Stornierte Buchungen sind nicht mehr aenderbar - die Angabe liesse sich nicht nachtragen. */
    @Test
    void stornierteBuchungWirdNichtGeprueft() {
        Buchung b = ausgabeMitGegenkonto();
        b.setStorniert(true);

        assertFalse(b.fehlendeRechnungsnummer());
    }

    private Buchung ausgabeMitGegenkonto() {
        Buchung b = new Buchung();
        b.setBetragEuro(new BigDecimal("-42.50"));
        b.setGegenkontoName("Musterverlag GmbH");
        b.setGegenkontoIban("DE02120300000000202051");
        return b;
    }
}
