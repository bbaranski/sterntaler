package de.foerderverein.kasse.hbci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.kapott.hbci.callback.AbstractHBCICallback;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;

import de.foerderverein.kasse.domain.Konto;

/** Regressionstest fuer den Bug "No passport implementation '/pfad/zur/datei' found":
 * AbstractHBCIPassport.getInstance(String) interpretiert das Argument als Passport-TYPNAMEN
 * (z. B. "PinTan"), nicht als Dateipfad. Der Dateipfad muss ueber die Object-Overload
 * uebergeben werden, siehe HbciImportService. */
class PassportCreationTest {

    @Test
    void getInstanceMitStringArgumentSchlaegtFehl() throws IOException {
        Path passportFile = Files.createTempDirectory("kasse-passport-test").resolve("passport.dat");
        HBCIUtils.init(new Properties(), dummyCallback());
        try {
            HBCIUtils.setParam("client.passport.default", "PinTan");
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                    () -> AbstractHBCIPassport.getInstance(passportFile.toString()));
        } finally {
            HBCIUtils.done();
        }
    }

    @Test
    void getInstanceMitObjectArgumentErzeugtPinTanPassport() throws IOException {
        Path passportFile = Files.createTempDirectory("kasse-passport-test").resolve("passport.dat");
        Konto konto = new Konto();
        konto.setHbciBlz("49450120"); // Sparkasse Herford - realer Eintrag aus blz.properties
        konto.setHbciUserId("test-user");
        HBCIUtils.init(new Properties(), new HbciCallbackAdapter(konto, "1234", "test-passphrase", null));
        HBCIPassport passport = null;
        try {
            HBCIUtils.setParam("client.passport.default", "PinTan");
            // Muss ein java.io.File sein: HBCIPassportPinTan(Object) verwendet den Dateipfad nur,
            // wenn init instanceof File ist - bei einem blossen String faellt es auf den (hier nie
            // gesetzten) Kernel-Parameter "client.passport.PinTan.filename" zurueck und wirft eine NPE.
            Object init = passportFile.toFile();

            passport = AbstractHBCIPassport.getInstance(init);

            assertNotNull(passport);
            assertEquals("org.kapott.hbci.passport.HBCIPassportPinTan", passport.getClass().getName());
        } finally {
            if (passport != null) {
                passport.close();
            }
            HBCIUtils.done();
        }
    }

    private AbstractHBCICallback dummyCallback() {
        return new AbstractHBCICallback() {
            @Override
            public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData) {
                if (reason == org.kapott.hbci.callback.HBCICallback.NEED_PASSPHRASE_LOAD
                        || reason == org.kapott.hbci.callback.HBCICallback.NEED_PASSPHRASE_SAVE) {
                    retData.replace(0, retData.length(), "test-passphrase");
                }
            }

            @Override
            public void status(HBCIPassport passport, int statusTag, Object[] o) {
            }

            @Override
            public void log(String msg, int level, Date date, StackTraceElement trace) {
            }
        };
    }
}
