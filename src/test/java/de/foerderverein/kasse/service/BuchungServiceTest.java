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
                new GeschaeftsjahrService(geschaeftsjahrRepository), new AuditLogRepository(database),
                kategorieRepository, new AktivitaetRepository(database));
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

    @Test
    void doppelteRechnungsnummerImSelbenGeschaeftsjahrWirdAbgelehnt() {
        Geschaeftsjahr jahr = new Geschaeftsjahr();
        jahr.setBezeichnung("2026");
        jahr.setStartDatum(LocalDate.of(2026, 1, 1));
        jahr.setEndDatum(LocalDate.of(2026, 12, 31));
        geschaeftsjahrRepository.save(jahr);

        Buchung erste = buchung(new BigDecimal("10.00"));
        erste.setRechnungsnummer("RE-042");
        buchungService.erfasseManuell(erste, "tester");

        Buchung zweite = buchung(new BigDecimal("20.00"));
        zweite.setRechnungsnummer("RE-042");

        assertThrows(IllegalStateException.class, () -> buchungService.erfasseManuell(zweite, "tester"));
    }

    @Test
    void gleicheRechnungsnummerInUnterschiedlichenGeschaeftsjahrenIstErlaubt() {
        Geschaeftsjahr jahr2025 = new Geschaeftsjahr();
        jahr2025.setBezeichnung("2025");
        jahr2025.setStartDatum(LocalDate.of(2025, 1, 1));
        jahr2025.setEndDatum(LocalDate.of(2025, 12, 31));
        geschaeftsjahrRepository.save(jahr2025);
        Geschaeftsjahr jahr2026 = new Geschaeftsjahr();
        jahr2026.setBezeichnung("2026");
        jahr2026.setStartDatum(LocalDate.of(2026, 1, 1));
        jahr2026.setEndDatum(LocalDate.of(2026, 12, 31));
        geschaeftsjahrRepository.save(jahr2026);

        Buchung erste = buchung(new BigDecimal("10.00"));
        erste.setBuchungsdatum(LocalDate.of(2025, 6, 1));
        erste.setRechnungsnummer("RE-001");
        buchungService.erfasseManuell(erste, "tester");

        Buchung zweite = buchung(new BigDecimal("20.00"));
        zweite.setBuchungsdatum(LocalDate.of(2026, 6, 1));
        zweite.setRechnungsnummer("RE-001");

        buchungService.erfasseManuell(zweite, "tester");

        assertEquals(2, buchungService.findAll().size());
    }

    @Test
    void aktualisiereOhneAenderungDerEigenenRechnungsnummerIstErlaubt() {
        Buchung buchung = buchung(new BigDecimal("10.00"));
        buchung.setRechnungsnummer("RE-042");
        buchungService.erfasseManuell(buchung, "tester");

        buchung.setVerwendungszweck("Geaendert");
        buchungService.aktualisiere(buchung);

        assertEquals("RE-042", buchungService.findAll().get(0).getRechnungsnummer());
    }

    @Test
    void umhaengenInEinAbgeschlossenesJahrWirdAbgelehnt() {
        Geschaeftsjahr offen = geschaeftsjahrRepository.save(
                jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false));
        Geschaeftsjahr abgeschlossen = geschaeftsjahrRepository.save(
                jahr("2025", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), true));

        Buchung buchung = buchung(new BigDecimal("10.00"));
        buchungService.erfasseManuell(buchung, "tester");
        assertEquals(offen.getId(), geschaeftsjahrRepository.findAll().stream()
                .filter(g -> g.getBezeichnung().equals("2026")).findFirst().orElseThrow().getId());

        buchung.setGeschaeftsjahrId(abgeschlossen.getId());

        assertThrows(IllegalStateException.class, () -> buchungService.aktualisiere(buchung));
    }

    @Test
    void umhaengenAusEinemAbgeschlossenenJahrHerausWirdAbgelehnt() {
        geschaeftsjahrRepository.save(jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false));

        Buchung buchung = buchung(new BigDecimal("10.00"));
        buchungService.erfasseManuell(buchung, "tester");

        // Jahr erst nach dem Anlegen abschliessen, sonst waere schon das Erfassen gesperrt.
        Geschaeftsjahr jahr2026 = geschaeftsjahrRepository.findAll().stream()
                .filter(g -> g.getBezeichnung().equals("2026")).findFirst().orElseThrow();
        jahr2026.setAbgeschlossen(true);
        geschaeftsjahrRepository.save(jahr2026);
        Geschaeftsjahr ziel = geschaeftsjahrRepository.save(
                jahr("2027", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), false));

        buchung.setGeschaeftsjahrId(ziel.getId());

        assertThrows(IllegalStateException.class, () -> buchungService.aktualisiere(buchung));
    }

    @Test
    void umhaengenZwischenOffenenJahrenIstErlaubt() {
        geschaeftsjahrRepository.save(jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false));
        Geschaeftsjahr ziel = geschaeftsjahrRepository.save(
                jahr("2027", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), false));

        Buchung buchung = buchung(new BigDecimal("10.00"));
        buchungService.erfasseManuell(buchung, "tester");
        buchung.setGeschaeftsjahrId(ziel.getId());
        buchungService.aktualisiere(buchung);

        assertEquals(ziel.getId(), buchungService.findAll().get(0).getGeschaeftsjahrId());
    }

    @Test
    void umhaengenWirdImVerlaufProtokolliert() {
        geschaeftsjahrRepository.save(jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false));
        Geschaeftsjahr ziel = geschaeftsjahrRepository.save(
                jahr("2027", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), false));

        Buchung buchung = buchung(new BigDecimal("10.00"));
        buchungService.erfasseManuell(buchung, "tester");
        buchung.setGeschaeftsjahrId(ziel.getId());
        buchungService.aktualisiere(buchung);

        var geaendert = buchungService.findVerlauf(buchung.getId()).stream()
                .filter(e -> "Geschäftsjahr".equals(e.getFeld()))
                .findFirst().orElseThrow();
        assertEquals("2026", geaendert.getWertAlt());
        assertEquals("2027 (fest zugeordnet)", geaendert.getWertNeu());
    }

    @Test
    void rechnungsnummerEindeutigkeitFolgtDemUmgehaengtenJahr() {
        geschaeftsjahrRepository.save(jahr("2026", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), false));
        Geschaeftsjahr ziel = geschaeftsjahrRepository.save(
                jahr("2027", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31), false));

        Buchung erste = buchung(new BigDecimal("10.00"));
        erste.setRechnungsnummer("RE-042");
        buchungService.erfasseManuell(erste, "tester");

        Buchung zweite = buchung(new BigDecimal("20.00"));
        zweite.setRechnungsnummer("RE-042");
        // Im selben Jahr waere das ein Konflikt - umgehaengt ins Folgejahr nicht mehr.
        zweite.setGeschaeftsjahrId(ziel.getId());
        buchungService.erfasseManuell(zweite, "tester");

        assertEquals(2, buchungService.findAll().size());
    }

    private Geschaeftsjahr jahr(String bezeichnung, LocalDate start, LocalDate end, boolean abgeschlossen) {
        Geschaeftsjahr jahr = new Geschaeftsjahr();
        jahr.setBezeichnung(bezeichnung);
        jahr.setStartDatum(start);
        jahr.setEndDatum(end);
        jahr.setAbgeschlossen(abgeschlossen);
        return jahr;
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
