package de.foerderverein.kasse.hbci;

import org.kapott.hbci.callback.AbstractHBCICallback;
import org.kapott.hbci.callback.HBCICallback;
import org.kapott.hbci.manager.HBCIUtils;
import org.kapott.hbci.passport.HBCIPassport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.foerderverein.kasse.domain.Konto;

/** Beantwortet die von hbci4j waehrend eines PIN/TAN-Dialogs gestellten Rueckfragen aus den bereits
 * bekannten Verbindungsdaten (Konto, PIN, Passport-Passphrase). Fuer TAN-Abfragen - die bei reinem
 * Lesezugriff nur selten vorkommen, siehe {@link TanCallback} - wird an den Aufrufer delegiert. */
class HbciCallbackAdapter extends AbstractHBCICallback {

    private static final Logger LOG = LoggerFactory.getLogger(HbciCallbackAdapter.class);

    private final Konto konto;
    private final String pin;
    private final String passportPassphrase;
    private final TanCallback tanCallback;

    HbciCallbackAdapter(Konto konto, String pin, String passportPassphrase, TanCallback tanCallback) {
        this.konto = konto;
        this.pin = pin;
        this.passportPassphrase = passportPassphrase;
        this.tanCallback = tanCallback;
    }

    @Override
    public void callback(HBCIPassport passport, int reason, String msg, int datatype, StringBuffer retData) {
        switch (reason) {
            case HBCICallback.NEED_COUNTRY -> set(retData, "DE");
            case HBCICallback.NEED_BLZ -> set(retData, konto.getHbciBlz());
            case HBCICallback.NEED_HOST -> set(retData, HBCIUtils.getBankInfo(konto.getHbciBlz()).getPinTanAddress());
            case HBCICallback.NEED_PORT -> set(retData, "443");
            case HBCICallback.NEED_FILTER -> set(retData, "Base64");
            case HBCICallback.NEED_USERID -> set(retData, konto.getHbciUserId());
            case HBCICallback.NEED_CUSTOMERID -> set(retData,
                    konto.getHbciCustomerId() != null && !konto.getHbciCustomerId().isBlank()
                            ? konto.getHbciCustomerId()
                            : konto.getHbciUserId());
            case HBCICallback.NEED_PASSPHRASE_LOAD, HBCICallback.NEED_PASSPHRASE_SAVE -> set(retData, passportPassphrase);
            case HBCICallback.NEED_PT_PIN -> set(retData, pin);
            case HBCICallback.NEED_PT_TAN -> {
                if (tanCallback == null) {
                    throw new HbciException("Die Bank verlangt eine TAN, es ist aber keine TAN-Eingabe verfuegbar.");
                }
                set(retData, tanCallback.promptTan(msg));
            }
            case HBCICallback.HAVE_ERROR -> LOG.warn("HBCI-Fehler: {}", msg);
            default -> {
                // Unbehandelte Rueckfragen: den von hbci4j vorgeschlagenen Default in retData unveraendert lassen.
                LOG.debug("Unbehandelter HBCI-Callback reason={} msg={}", reason, msg);
            }
        }
    }

    @Override
    public void status(HBCIPassport passport, int statusTag, Object[] o) {
        LOG.debug("HBCI-Status {}: {}", statusTag, o != null && o.length > 0 ? o[0] : "");
    }

    @Override
    public void log(String msg, int level, java.util.Date date, StackTraceElement trace) {
        LOG.debug("[HBCI4Java] {}", msg);
    }

    private void set(StringBuffer retData, String value) {
        retData.replace(0, retData.length(), value != null ? value : "");
    }
}
