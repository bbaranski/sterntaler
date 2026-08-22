package de.foerderverein.kasse.hbci;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.kapott.hbci.callback.AbstractHBCICallback;
import org.kapott.hbci.manager.BankInfo;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassport;

/** Regressionstest fuer den Bug "Zu BLZ ... ist keine FinTS-Adresse bekannt" bei jeder BLZ:
 * HBCIUtils.getBankInfo(...) liefert nur dann etwas, wenn HBCIUtils.init(...) zuvor die
 * mitgelieferte blz.properties geladen hat (siehe HbciImportService/HbciCallbackAdapter). */
class BlzLookupTest {

    @Test
    void ohneInitIstJedeBlzUnbekannt() {
        // Bewusst KEIN HBCIUtils.init() vorher - reproduziert den urspruenglichen Bug.
        assertNull(HBCIUtils.getBankInfo("49450120"));
    }

    @Test
    void nachInitWirdEineEchteSparkassenBlzGefunden() {
        HBCIUtils.init(new Properties(), new AbstractHBCICallback() {
            @Override
            public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData) {
                // keine Interaktion in diesem Test noetig
            }

            @Override
            public void status(HBCIPassport passport, int statusTag, Object[] o) {
            }

            @Override
            public void log(String msg, int level, Date date, StackTraceElement trace) {
            }
        });

        // Sparkasse Herford - ein realer Eintrag aus der mitgelieferten blz.properties.
        try {
            BankInfo bankInfo = HBCIUtils.getBankInfo("49450120");

            assertNotNull(bankInfo);
            assertNotNull(bankInfo.getPinTanAddress());
        } finally {
            HBCIUtils.done();
        }
    }
}
