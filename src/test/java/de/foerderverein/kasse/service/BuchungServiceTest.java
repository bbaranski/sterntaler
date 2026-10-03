package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.AuditAktion;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.BuchungsQuelle;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class BuchungServiceTest {

    private BuchungService buchungService;
    private GeschaeftsjahrRepository geschaeftsjahrRepository;
    private KategorieRepository kategorieRepository;
    private Konto konto;

    @BeforeEach
    void setUp() {
        Database database = TestDatabase.create();
        geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        kategorieRepository = new KategorieRepository(database);
        buchungService = new BuchungService(new BuchungRepository(database), geschaeftsjahrRepository,
                new AuditLogRepository(database), kategorieRepository, new AktivitaetRepository(database));
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

    @Test
    void erfasseManuellSchreibtAuditEintrag() {
        Buchung buchung = buchung(new BigDecimal("10.00"));

        buchungService.erfasseManuell(buchung, "tester");

        var verlauf = buchungService.findVerlauf(buchung.getId());
        assertEquals(1, verlauf.size());
        assertEquals(AuditAktion.ANGELEGT, verlauf.get(0).getAktion());
        assertEquals("tester", verlauf.get(0).getBenutzer());
    }

    @Test
    void aktualisiereUndStornioreSchreibenAuditEintraege() {
        Buchung buchung = buchung(new BigDecimal("10.00"));
        buchungService.erfasseManuell(buchung, "tester");

        buchung.setVerwendungszweck("Geaendert");
        buchungService.aktualisiere(buchung);
        buchungService.storniere(buchung);

        var verlauf = buchungService.findVerlauf(buchung.getId());
        assertEquals(3, verlauf.size());
        assertTrue(verlauf.stream().anyMatch(e -> e.getAktion() == AuditAktion.GEAENDERT));
        assertTrue(verlauf.stream().anyMatch(e -> e.getAktion() == AuditAktion.STORNIERT));
    }

    @Test
    void aktualisiereProtokolliertFeldDifferenzenMitAufgeloestenNamen() {
        Kategorie kategorie = kategorieRepository.findAll().get(0);
        Buchung buchung = buchung(new BigDecimal("10.00"));
        buchungService.erfasseManuell(buchung, "tester");

        buchung.setVerwendungszweck("Geaendert");
        buchung.setKategorieId(kategorie.getId());
        buchungService.aktualisiere(buchung);

        var geaendert = buchungService.findVerlauf(buchung.getId()).stream()
                .filter(e -> e.getAktion() == AuditAktion.GEAENDERT)
                .toList();
        assertEquals(2, geaendert.size());
        assertTrue(geaendert.stream().anyMatch(e -> "Verwendungszweck".equals(e.getFeld())
                && "".equals(e.getWertAlt()) && "Geaendert".equals(e.getWertNeu())));
        assertTrue(geaendert.stream().anyMatch(e -> "Kategorie".equals(e.getFeld())
                && "".equals(e.getWertAlt()) && kategorie.getName().equals(e.getWertNeu())));
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
