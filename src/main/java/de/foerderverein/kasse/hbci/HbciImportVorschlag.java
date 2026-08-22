package de.foerderverein.kasse.hbci;

import de.foerderverein.kasse.domain.Buchung;

/** Ein per HBCI abgerufener, noch nicht gespeicherter Umsatz. {@code bereitsVorhanden} markiert
 * Umsaetze, die anhand des Import-Hashes vermutlich schon frueher importiert wurden - die
 * Review-UI zeigt sie an, aber vorbelegt als nicht erneut zu uebernehmen. */
public record HbciImportVorschlag(Buchung buchung, boolean bereitsVorhanden) {
}
