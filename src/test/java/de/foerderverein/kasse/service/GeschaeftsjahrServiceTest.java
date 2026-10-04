package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class GeschaeftsjahrServiceTest {

    @Test
    void erstellenSetztFelder() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));

        Geschaeftsjahr jahr = service.erstellen("2025/2026", LocalDate.of(2025, 9, 1), LocalDate.of(2026, 8, 31),
                new BigDecimal("500.00"));

        assertEquals("2025/2026", jahr.getBezeichnung());
        assertEquals(LocalDate.of(2025, 9, 1), jahr.getStartDatum());
        assertEquals(LocalDate.of(2026, 8, 31), jahr.getEndDatum());
        assertEquals(50000, jahr.getStartsaldoCent());
    }

    @Test
    void doppelteBezeichnungWirdAbgelehnt() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        service.erstellen("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> service.erstellen(
                "2026", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), BigDecimal.ZERO));
    }

    @Test
    void ueberlappenderZeitraumWirdAbgelehnt() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        service.erstellen("A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> service.erstellen(
                "B", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 12, 31), BigDecimal.ZERO));
    }

    @Test
    void direktAngrenzenderZeitraumWirdErlaubt() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        service.erstellen("A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), BigDecimal.ZERO);

        Geschaeftsjahr b = service.erstellen("B", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), BigDecimal.ZERO);

        assertEquals("B", b.getBezeichnung());
    }

    @Test
    void gleicherTagAlsGrenzeWirdAbgelehnt() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        service.erstellen("A", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> service.erstellen(
                "B", LocalDate.of(2026, 6, 30), LocalDate.of(2026, 12, 31), BigDecimal.ZERO));
    }

    @Test
    void aktualisiereAufAbgeschlossenemJahrWirdAbgelehnt() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = service.erstellen("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.ZERO);
        service.abschliessen(jahr, LocalDate.of(2027, 1, 15), "Max Mustermann");

        assertThrows(IllegalStateException.class, () -> service.aktualisiere(
                jahr, "2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 30), BigDecimal.ZERO));
    }

    @Test
    void abschliessenSetztFelder() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = service.erstellen("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), BigDecimal.ZERO);

        service.abschliessen(jahr, LocalDate.of(2027, 2, 1), "Max Mustermann");

        assertTrue(jahr.isAbgeschlossen());
        assertEquals("Max Mustermann", jahr.getKassenpruefer());
    }

    @Test
    void findeFuerLiefertPassendesJahr() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0);

        assertTrue(service.findeFuer(LocalDate.of(2026, 6, 1), List.of(jahr)).isPresent());
        assertTrue(service.findeFuer(LocalDate.of(2025, 12, 31), List.of(jahr)).isEmpty());
    }

    @Test
    void berechneLaufendeSaldenKumuliertAbStartsaldo() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 10000);
        jahr.setId(1L);

        Buchung b1 = buchung(1L, 1L, LocalDate.of(2026, 1, 5), 5000, false);
        Buchung b2 = buchung(2L, 1L, LocalDate.of(2026, 1, 10), -2000, false);

        Map<Long, Long> salden = service.berechneLaufendeSalden(List.of(b1, b2), List.of(jahr));

        assertEquals(15000, salden.get(1L));
        assertEquals(13000, salden.get(2L));
    }

    @Test
    void berechneLaufendeSaldenSchliesstStornierteAus() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0);
        jahr.setId(1L);

        Buchung storniert = buchung(1L, 1L, LocalDate.of(2026, 1, 5), 5000, true);

        Map<Long, Long> salden = service.berechneLaufendeSalden(List.of(storniert), List.of(jahr));

        assertFalse(salden.containsKey(1L));
    }

    @Test
    void berechneLaufendeSaldenSchliesstBuchungOhneZuordnungAus() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0);
        jahr.setId(1L);

        Buchung ausserhalb = buchung(1L, 1L, LocalDate.of(2027, 1, 5), 5000, false);

        Map<Long, Long> salden = service.berechneLaufendeSalden(List.of(ausserhalb), List.of(jahr));

        assertFalse(salden.containsKey(1L));
    }

    @Test
    void berechneLaufendeSaldenNutztIdAlsTiebreakBeiGleichemDatum() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0);
        jahr.setId(1L);

        Buchung erste = buchung(1L, 1L, LocalDate.of(2026, 1, 5), 1000, false);
        Buchung zweite = buchung(2L, 1L, LocalDate.of(2026, 1, 5), 2000, false);

        Map<Long, Long> salden = service.berechneLaufendeSalden(List.of(zweite, erste), List.of(jahr));

        assertEquals(1000, salden.get(1L));
        assertEquals(3000, salden.get(2L));
    }

    @Test
    void berechneLaufendeSaldenGruppiertProKonto() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 1000);
        jahr.setId(1L);

        Buchung kontoA = buchung(1L, 10L, LocalDate.of(2026, 1, 5), 500, false);
        Buchung kontoB = buchung(2L, 20L, LocalDate.of(2026, 1, 5), 300, false);

        Map<Long, Long> salden = service.berechneLaufendeSalden(List.of(kontoA, kontoB), List.of(jahr));

        assertEquals(1500, salden.get(1L));
        assertEquals(1300, salden.get(2L));
    }

    @Test
    void stichtageGehoerenOhneUebersteuerungZumJahr() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0);

        assertTrue(service.findeFuer(LocalDate.of(2026, 1, 1), List.of(jahr)).isPresent());
        assertTrue(service.findeFuer(LocalDate.of(2026, 12, 31), List.of(jahr)).isPresent());
    }

    @Test
    void ausdrueckicheZuordnungGewinntGegenDasDatum() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr alt = jahr("2025", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), 0);
        alt.setId(1L);
        Geschaeftsjahr neu = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0);
        neu.setId(2L);

        Buchung amStichtag = buchung(1L, 1L, LocalDate.of(2025, 12, 31), 1000, false);
        assertEquals("2025", service.findeFuer(amStichtag, List.of(alt, neu)).orElseThrow().getBezeichnung());

        amStichtag.setGeschaeftsjahrId(2L);
        assertEquals("2026", service.findeFuer(amStichtag, List.of(alt, neu)).orElseThrow().getBezeichnung());

        amStichtag.setGeschaeftsjahrId(null);
        assertEquals("2025", service.findeFuer(amStichtag, List.of(alt, neu)).orElseThrow().getBezeichnung());
    }

    @Test
    void umgehaengteStichtagsbuchungVerschiebtBeideSaldoKetten() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr alt = jahr("2025", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), 10000);
        alt.setId(1L);
        Geschaeftsjahr neu = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 50000);
        neu.setId(2L);

        Buchung bleibt = buchung(1L, 1L, LocalDate.of(2025, 12, 31), 1000, false);
        Buchung wandert = buchung(2L, 1L, LocalDate.of(2025, 12, 31), 2000, false);
        Buchung imNeuenJahr = buchung(3L, 1L, LocalDate.of(2026, 1, 2), 500, false);

        Map<Long, Long> vorher = service.berechneLaufendeSalden(List.of(bleibt, wandert, imNeuenJahr), List.of(alt, neu));
        assertEquals(11000, vorher.get(1L));
        assertEquals(13000, vorher.get(2L));
        assertEquals(50500, vorher.get(3L));

        wandert.setGeschaeftsjahrId(2L);
        Map<Long, Long> nachher = service.berechneLaufendeSalden(List.of(bleibt, wandert, imNeuenJahr), List.of(alt, neu));

        // Vorjahr endet jetzt 2000 niedriger ...
        assertEquals(11000, nachher.get(1L));
        // ... und die umgehaengte Buchung startet die Kette des Folgejahres.
        assertEquals(52000, nachher.get(2L));
        assertEquals(52500, nachher.get(3L));
    }

    @Test
    void zuordnungAufUnbekanntesJahrLiefertKeinJahr() {
        GeschaeftsjahrService service = new GeschaeftsjahrService(new GeschaeftsjahrRepository(TestDatabase.create()));
        Geschaeftsjahr jahr = jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), 0);
        jahr.setId(1L);

        Buchung buchung = buchung(1L, 1L, LocalDate.of(2026, 6, 1), 1000, false);
        buchung.setGeschaeftsjahrId(99L);

        assertTrue(service.findeFuer(buchung, List.of(jahr)).isEmpty());
        assertTrue(service.berechneLaufendeSalden(List.of(buchung), List.of(jahr)).isEmpty());
    }

    private Geschaeftsjahr jahr(String bezeichnung, LocalDate start, LocalDate end, long startsaldoCent) {
        Geschaeftsjahr g = new Geschaeftsjahr();
        g.setBezeichnung(bezeichnung);
        g.setStartDatum(start);
        g.setEndDatum(end);
        g.setStartsaldoCent(startsaldoCent);
        return g;
    }

    private Buchung buchung(long id, long kontoId, LocalDate datum, long betragCent, boolean storniert) {
        Buchung b = new Buchung();
        b.setId(id);
        b.setKontoId(kontoId);
        b.setBuchungsdatum(datum);
        b.setBetragCent(betragCent);
        b.setQuelle(BuchungsQuelle.MANUELL);
        b.setStorniert(storniert);
        return b;
    }
}
