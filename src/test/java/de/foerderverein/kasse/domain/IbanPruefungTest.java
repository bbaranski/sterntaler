package de.foerderverein.kasse.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IbanPruefungTest {

    @Test
    void gueltigeIbanWirdErkannt() {
        assertTrue(IbanPruefung.istGueltig("DE89370400440532013000"));
    }

    @Test
    void schreibweiseMitLeerzeichenUndKleinbuchstabenWirdToleriert() {
        assertTrue(IbanPruefung.istGueltig("de89 3704 0044 0532 0130 00"));
    }

    @Test
    void falschePruefsummeWirdErkannt() {
        assertFalse(IbanPruefung.istGueltig("DE89370400440532013001"));
    }

    @Test
    void zuKurzeZeichenketteIstUngueltig() {
        assertFalse(IbanPruefung.istGueltig("DE1234"));
    }

    @Test
    void nullIstUngueltig() {
        assertFalse(IbanPruefung.istGueltig(null));
    }
}
