package de.foerderverein.kasse.hbci;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.kapott.hbci.GV.HBCIJob;
import org.kapott.hbci.GV_Result.GVRKUms;
import org.kapott.hbci.manager.BankInfo;
import org.kapott.hbci.manager.HBCIHandler;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.manager.HBCIVersion;
import org.kapott.hbci.passport.AbstractHBCIPassport;
import org.kapott.hbci.passport.HBCIPassport;
import org.kapott.hbci.status.HBCIExecStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.foerderverein.kasse.config.AppConfig;
import de.foerderverein.kasse.domain.Konto;
import de.foerderverein.kasse.persistence.BuchungRepository;

/** Ruft ausschliesslich lesend Kontoumsaetze per HBCI/FinTS (PIN/TAN-Verfahren) ab.
 * Es werden bewusst keine Auftraege (Ueberweisungen) unterstuetzt - dafuer braucht es
 * regulaer keine TAN, siehe {@link TanCallback} fuer die (seltene) PSD2-Ausnahme.
 *
 * <p>Diese Methode fuehrt eine synchrone Netzwerkkommunikation durch und darf nicht auf dem
 * JavaFX-Application-Thread aufgerufen werden. */
public class HbciImportService {

    private static final Logger LOG = LoggerFactory.getLogger(HbciImportService.class);

    private final BuchungRepository buchungRepository;
    private final CredentialStore credentialStore;
    private final UmsatzMapper mapper = new UmsatzMapper();

    public HbciImportService(BuchungRepository buchungRepository, CredentialStore credentialStore) {
        this.buchungRepository = buchungRepository;
        this.credentialStore = credentialStore;
    }

    public List<HbciImportVorschlag> holeUmsaetze(Konto konto, String pin, LocalDate von, LocalDate bis,
                                                    TanCallback tanCallback) {
        if (!konto.isHbciEingerichtet()) {
            throw new HbciException("Fuer dieses Konto ist noch kein HBCI-Zugang eingerichtet.");
        }

        String passphrase = credentialStore.holeOderErzeugePassportPassphrase(konto.getId());
        HbciCallbackAdapter callback = new HbciCallbackAdapter(konto, pin, passphrase, tanCallback);

        // Wichtig: HBCIUtils.init() laedt erst die mitgelieferte Bankleitzahlen-Liste (blz.properties)
        // in den Speicher. getBankInfo() davor aufgerufen liefert fuer JEDE BLZ null.
        HBCIUtils.init(new Properties(), callback);
        HBCIPassport passport = null;
        HBCIHandler handle = null;
        try {
            BankInfo bankInfo = HBCIUtils.getBankInfo(konto.getHbciBlz());
            if (bankInfo == null || bankInfo.getPinTanAddress() == null) {
                throw new HbciException("Zu BLZ " + konto.getHbciBlz() + " ist keine FinTS-Adresse bekannt.");
            }

            HBCIUtils.setParam("client.passport.default", "PinTan");
            // Muss ueber die Object-Overload mit einem java.io.File laufen: getInstance(String)
            // interpretiert das Argument als Passport-Typnamen (z. B. "PinTan"), nicht als Dateipfad.
            // Und HBCIPassportPinTan(Object) nutzt den Dateipfad nur, wenn init instanceof File ist -
            // bei einem blossen String faellt es sonst auf den (hier nie gesetzten) Kernel-Parameter
            // "client.passport.PinTan.filename" zurueck und wirft eine NullPointerException.
            Object passportFileArg = passportFile(konto).toFile();
            passport = AbstractHBCIPassport.getInstance(passportFileArg);

            HBCIVersion version = bankInfo.getPinTanVersion() != null ? bankInfo.getPinTanVersion() : HBCIVersion.HBCI_300;
            handle = new HBCIHandler(version.getId(), passport);

            org.kapott.hbci.structures.Konto zielKonto = waehleKonto(passport.getAccounts(), konto.getIban());

            HBCIJob job = handle.newJob("KUmsAll");
            job.setParam("my", zielKonto);
            job.setParam("startdate", toDate(von));
            job.setParam("enddate", toDate(bis));
            job.addToQueue();

            HBCIExecStatus status = handle.execute();
            if (!status.isOK()) {
                throw new HbciException("HBCI-Abruf fehlgeschlagen: " + status.getErrorString());
            }

            GVRKUms result = (GVRKUms) job.getJobResult();
            if (!result.isOK()) {
                throw new HbciException("Umsatzabruf lieferte einen Fehler zurueck.");
            }

            List<HbciImportVorschlag> vorschlaege = new ArrayList<>();
            for (GVRKUms.UmsLine line : result.getFlatData()) {
                de.foerderverein.kasse.domain.Buchung buchung = mapper.toBuchung(line, konto.getId());
                boolean vorhanden = buchungRepository.existsByHbciHash(konto.getId(), buchung.getHbciImportHash());
                vorschlaege.add(new HbciImportVorschlag(buchung, vorhanden));
            }
            LOG.info("HBCI-Import: {} Umsaetze abgerufen, davon {} vermutlich bereits vorhanden",
                    vorschlaege.size(), vorschlaege.stream().filter(HbciImportVorschlag::bereitsVorhanden).count());
            return vorschlaege;
        } finally {
            if (handle != null) {
                handle.close();
            }
            if (passport != null) {
                passport.close();
            }
            HBCIUtils.done();
        }
    }

    private java.nio.file.Path passportFile(Konto konto) {
        return AppConfig.appDir().resolve("hbci-passport-" + konto.getId() + ".dat");
    }

    private org.kapott.hbci.structures.Konto waehleKonto(org.kapott.hbci.structures.Konto[] accounts, String iban) {
        if (accounts == null || accounts.length == 0) {
            throw new HbciException("Die Bank hat keine Konten zu diesem Zugang gemeldet.");
        }
        String normalizedIban = iban != null ? iban.replace(" ", "").toUpperCase() : "";
        for (org.kapott.hbci.structures.Konto k : accounts) {
            if (k.iban != null && k.iban.replace(" ", "").toUpperCase().equals(normalizedIban)) {
                return k;
            }
        }
        if (accounts.length == 1) {
            return accounts[0];
        }
        throw new HbciException("Keines der von der Bank gemeldeten Konten passt zur hinterlegten IBAN.");
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
