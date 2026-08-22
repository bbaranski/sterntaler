package de.foerderverein.kasse.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Geschaeftsjahr;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;

public class GeschaeftsjahrService {

    private final GeschaeftsjahrRepository repository;

    public GeschaeftsjahrService(GeschaeftsjahrRepository repository) {
        this.repository = repository;
    }

    public List<Geschaeftsjahr> findAll() {
        return repository.findAll();
    }

    public Geschaeftsjahr erstellen(String bezeichnung, LocalDate start, LocalDate end, BigDecimal startsaldo) {
        Geschaeftsjahr geschaeftsjahr = new Geschaeftsjahr();
        validiere(geschaeftsjahr, bezeichnung, start, end);
        geschaeftsjahr.setBezeichnung(bezeichnung);
        geschaeftsjahr.setStartDatum(start);
        geschaeftsjahr.setEndDatum(end);
        geschaeftsjahr.setStartsaldoEuro(startsaldo);
        return repository.save(geschaeftsjahr);
    }

    /** Aendert Bezeichnung/Zeitraum/Startsaldo eines bestehenden Geschaeftsjahres. Nur erlaubt,
     * solange es nicht abgeschlossen ist - sonst liesse sich rueckwirkend eine per
     * BuchungService.pruefeNichtGesperrt gesperrte Buchung entsperren, da die Zuordnung
     * Buchung->Geschaeftsjahr live aus dem Zeitraum abgeleitet wird und nicht gespeichert ist. */
    public void aktualisiere(Geschaeftsjahr geschaeftsjahr, String bezeichnung, LocalDate start, LocalDate end,
                              BigDecimal startsaldo) {
        if (geschaeftsjahr.isAbgeschlossen()) {
            throw new IllegalStateException("Ein abgeschlossenes Geschaeftsjahr kann nicht mehr geaendert werden.");
        }
        validiere(geschaeftsjahr, bezeichnung, start, end);
        geschaeftsjahr.setBezeichnung(bezeichnung);
        geschaeftsjahr.setStartDatum(start);
        geschaeftsjahr.setEndDatum(end);
        geschaeftsjahr.setStartsaldoEuro(startsaldo);
        repository.save(geschaeftsjahr);
    }

    /** Schliesst ein Geschaeftsjahr ab - danach sind Buchungen mit Datum in diesem Jahr gesperrt
     * (siehe BuchungService), bis es hier wieder geoeffnet wird. */
    public void abschliessen(Geschaeftsjahr geschaeftsjahr, LocalDate kassenpruefungDatum, String kassenpruefer) {
        geschaeftsjahr.setAbgeschlossen(true);
        geschaeftsjahr.setAbschlussdatum(LocalDate.now());
        geschaeftsjahr.setKassenpruefungDatum(kassenpruefungDatum);
        geschaeftsjahr.setKassenpruefer(kassenpruefer);
        repository.save(geschaeftsjahr);
    }

    public void wiederOeffnen(Geschaeftsjahr geschaeftsjahr) {
        geschaeftsjahr.setAbgeschlossen(false);
        repository.save(geschaeftsjahr);
    }

    private void validiere(Geschaeftsjahr geschaeftsjahr, String bezeichnung, LocalDate start, LocalDate end) {
        if (bezeichnung == null || bezeichnung.isBlank()) {
            throw new IllegalArgumentException("Bezeichnung darf nicht leer sein");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start- und Enddatum muessen gesetzt sein");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Enddatum darf nicht vor dem Startdatum liegen");
        }
        for (Geschaeftsjahr andere : repository.findAll()) {
            if (andere.getId() != null && andere.getId().equals(geschaeftsjahr.getId())) {
                continue;
            }
            if (andere.getBezeichnung().equalsIgnoreCase(bezeichnung)) {
                throw new IllegalArgumentException("Geschaeftsjahr \"" + bezeichnung + "\" existiert bereits");
            }
            if (!start.isAfter(andere.getEndDatum()) && !end.isBefore(andere.getStartDatum())) {
                throw new IllegalArgumentException("Zeitraum ueberschneidet sich mit \"" + andere.getBezeichnung()
                        + "\" (" + andere.getStartDatum() + " - " + andere.getEndDatum() + ")");
            }
        }
    }

    /** Liefert das Geschaeftsjahr, in dessen Zeitraum das Datum faellt, sofern eines existiert.
     * Reine Funktion ohne Datenbankzugriff, damit Aufrufer eine konsistente Momentaufnahme
     * verwenden koennen (siehe berechneLaufendeSalden). */
    public Optional<Geschaeftsjahr> findeFuer(LocalDate datum, List<Geschaeftsjahr> geschaeftsjahre) {
        return geschaeftsjahre.stream().filter(g -> g.enthaelt(datum)).findFirst();
    }

    /** Berechnet je Buchung das laufende Saldo: ausgehend vom Startsaldo des Geschaeftsjahres,
     * in dessen Zeitraum die Buchung faellt, kumuliert je (Konto, Geschaeftsjahr)-Gruppe in
     * chronologischer Reihenfolge (Buchungsdatum, dann Id als stabiler Tiebreak). Stornierte
     * Buchungen und Buchungen ausserhalb jedes Geschaeftsjahres tauchen nicht im Ergebnis auf. */
    public Map<Long, Long> berechneLaufendeSalden(List<Buchung> buchungen, List<Geschaeftsjahr> geschaeftsjahre) {
        record GruppenSchluessel(Long kontoId, Long geschaeftsjahrId) {
        }

        Map<Long, Geschaeftsjahr> geschaeftsjahreById = new HashMap<>();
        for (Geschaeftsjahr g : geschaeftsjahre) {
            geschaeftsjahreById.put(g.getId(), g);
        }

        Map<GruppenSchluessel, List<Buchung>> gruppen = new LinkedHashMap<>();
        for (Buchung b : buchungen) {
            if (b.isStorniert()) {
                continue;
            }
            Optional<Geschaeftsjahr> jahr = findeFuer(b.getBuchungsdatum(), geschaeftsjahre);
            if (jahr.isEmpty()) {
                continue;
            }
            GruppenSchluessel schluessel = new GruppenSchluessel(b.getKontoId(), jahr.get().getId());
            gruppen.computeIfAbsent(schluessel, k -> new ArrayList<>()).add(b);
        }

        Map<Long, Long> ergebnis = new HashMap<>();
        for (Map.Entry<GruppenSchluessel, List<Buchung>> eintrag : gruppen.entrySet()) {
            Geschaeftsjahr jahr = geschaeftsjahreById.get(eintrag.getKey().geschaeftsjahrId());
            List<Buchung> sortiert = eintrag.getValue().stream()
                    .sorted(Comparator.comparing(Buchung::getBuchungsdatum).thenComparing(Buchung::getId))
                    .toList();
            long laufend = jahr.getStartsaldoCent();
            for (Buchung b : sortiert) {
                laufend += b.getBetragCent();
                ergebnis.put(b.getId(), laufend);
            }
        }
        return ergebnis;
    }
}
