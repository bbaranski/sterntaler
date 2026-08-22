package de.foerderverein.kasse.hbci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.structures.Konto;
import org.kapott.hbci.structures.Value;

import de.foerderverein.kasse.domain.Buchung;

class UmsatzMapperTest {

    private final UmsatzMapper mapper = new UmsatzMapper();

    @Test
    void mapptGrundlegendeFelder() {
        GVRKUms.UmsLine line = new GVRKUms.UmsLine();
        line.bdate = new Date(2026 - 1900, 2, 1);
        line.valuta = new Date(2026 - 1900, 2, 3);
        line.value = new Value(new BigDecimal("-12.34"), "EUR");
        line.addUsage("Vereinsbedarf Getraenke");
        line.other = new Konto();
        line.other.iban = "DE02120300000000202051";
        line.other.name = "Getraenkemarkt";

        Buchung buchung = mapper.toBuchung(line, 1L);

        assertEquals(1L, buchung.getKontoId());
        assertEquals(-1234L, buchung.getBetragCent());
        assertEquals("EUR", buchung.getWaehrung());
        assertEquals("Vereinsbedarf Getraenke", buchung.getVerwendungszweck());
        assertEquals("DE02120300000000202051", buchung.getGegenkontoIban());
        assertEquals("Getraenkemarkt", buchung.getGegenkontoName());
        assertNotNull(buchung.getHbciImportHash());
    }

    @Test
    void gleicheEingabeErzeugtGleichenHash() {
        GVRKUms.UmsLine line1 = zeile("instref-1");
        GVRKUms.UmsLine line2 = zeile("instref-1");

        Buchung b1 = mapper.toBuchung(line1, 1L);
        Buchung b2 = mapper.toBuchung(line2, 1L);

        assertEquals(b1.getHbciImportHash(), b2.getHbciImportHash());
    }

    @Test
    void unterschiedlicheInstrefErzeugtUnterschiedlichenHash() {
        Buchung b1 = mapper.toBuchung(zeile("instref-1"), 1L);
        Buchung b2 = mapper.toBuchung(zeile("instref-2"), 1L);

        org.junit.jupiter.api.Assertions.assertNotEquals(b1.getHbciImportHash(), b2.getHbciImportHash());
    }

    private GVRKUms.UmsLine zeile(String instref) {
        GVRKUms.UmsLine line = new GVRKUms.UmsLine();
        line.bdate = new Date(2026 - 1900, 2, 1);
        line.valuta = new Date(2026 - 1900, 2, 1);
        line.value = new Value(new BigDecimal("50.00"), "EUR");
        line.usage = List.of("Spende");
        line.instref = instref;
        return line;
    }
}
