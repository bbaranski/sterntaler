package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class BuchungServiceTest {

    private BuchungService buchungService;
    private GeschaeftsjahrRepository geschaeftsjahrRepository;
    private Konto konto;

    @BeforeEach
    void setUp() {
        Database database = TestDatabase.create();
        geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        buchungService = new BuchungService(new BuchungRepository(database), geschaeftsjahrRepository);
        Konto neu = new Konto();
        neu.setBezeichnung("Vereinskonto");
        neu.setIban("DE02120300000000202051");
        konto = new KontoRepository(database).save(neu);
    }

    @Test
    void betragNullWirdAbgelehnt() {
        Buchung buchung = buchung(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> buchungService.erfasseManuell(buchung, "tester"));
    }

    @Test
    void importUeberspringtBereitsVorhandeneHbciBuchung() {
        Buchung erste = buchung(new BigDecimal("10.00"));
        erste.setHbciImportHash("abc");
        Buchung zweiteMitGleichemHash = buchung(new BigDecimal("10.00"));
        zweiteMitGleichemHash.setHbciImportHash("abc");

        int ersterImport = buchungService.importiereHbciBuchungen(List.of(erste));
        int zweiterImport = buchungService.importiereHbciBuchungen(List.of(zweiteMitGleichemHash));

        assertEquals(1, ersterImport);
        assertEquals(0, zweiterImport);
        assertEquals(1, buchungService.findAll().size());
    }

    @Test
    void buchungInAbgeschlossenemJahrWirdAbgelehnt() {
        Geschaeftsjahr jahr = new Geschaeftsjahr();
        jahr.setBezeichnung("2026");
        jahr.setStartDatum(LocalDate.of(2026, 1, 1));
        jahr.setEndDatum(LocalDate.of(2026, 12, 31));
        jahr.setAbgeschlossen(true);
        geschaeftsjahrRepository.save(jahr);

        Buchung buchung = buchung(new BigDecimal("10.00"));

        assertThrows(IllegalStateException.class, () -> buchungService.erfasseManuell(buchung, "tester"));
    }

    @Test
    void hbciImportUeberspringtGesperrtesJahr() {
        Geschaeftsjahr jahr = new Geschaeftsjahr();
        jahr.setBezeichnung("2026");
        jahr.setStartDatum(LocalDate.of(2026, 1, 1));
        jahr.setEndDatum(LocalDate.of(2026, 12, 31));
        jahr.setAbgeschlossen(true);
        geschaeftsjahrRepository.save(jahr);

        Buchung buchung = buchung(new BigDecimal("10.00"));

        int importiert = buchungService.importiereHbciBuchungen(List.of(buchung));

        assertEquals(0, importiert);
    }

    private Buchung buchung(BigDecimal betrag) {
        Buchung buchung = new Buchung();
        buchung.setKontoId(konto.getId());
        buchung.setBuchungsdatum(LocalDate.of(2026, 3, 1));
        buchung.setBetragEuro(betrag);
        buchung.setQuelle(BuchungsQuelle.MANUELL);
        return buchung;
    }
}
