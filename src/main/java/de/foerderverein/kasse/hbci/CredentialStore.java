package de.foerderverein.kasse.hbci;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;

/** Ablage der HBCI-Geheimnisse (PIN, Passport-Verschluesselungs-Passphrase) im OS-Schluesselbund
 * (Windows Credential Manager / macOS Keychain) statt in der Anwendungsdatenbank. */
public class CredentialStore {

    /** Traegt weiter den alten Programmnamen, obwohl die Anwendung seit 1.0.0 "Sterntaler"
     * heisst: Unter diesem Schluessel liegen die bereits gespeicherten PINs im Schluesselbund.
     * Ein Umbenennen wuerde sie unauffindbar machen und die Nutzer muessten sie neu eingeben. */
    private static final String DOMAIN = "Foerderverein-Kasse-HBCI";
    private static final SecureRandom RANDOM = new SecureRandom();

    public void speicherePin(long kontoId, String pin) {
        speichere(pinAccount(kontoId), pin);
    }

    public Optional<String> holePin(long kontoId) {
        return hole(pinAccount(kontoId));
    }

    public void loeschePin(long kontoId) {
        loesche(pinAccount(kontoId));
    }

    /** Liefert die Passphrase zur Verschluesselung der lokalen HBCI-Passport-Datei.
     * Wird beim ersten Zugriff je Konto zufaellig erzeugt und danach wiederverwendet. */
    public String holeOderErzeugePassportPassphrase(long kontoId) {
        String account = passphraseAccount(kontoId);
        return hole(account).orElseGet(() -> {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String passphrase = Base64.getEncoder().encodeToString(bytes);
            speichere(account, passphrase);
            return passphrase;
        });
    }

    private void speichere(String account, String wert) {
        try (Keyring keyring = Keyring.create()) {
            keyring.setPassword(DOMAIN, account, wert);
        } catch (Exception e) {
            throw new HbciException("Zugangsdaten konnten nicht im Schluesselbund gespeichert werden", e);
        }
    }

    private Optional<String> hole(String account) {
        try (Keyring keyring = Keyring.create()) {
            return Optional.of(keyring.getPassword(DOMAIN, account));
        } catch (PasswordAccessException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new HbciException("Zugangsdaten konnten nicht aus dem Schluesselbund gelesen werden", e);
        }
    }

    private void loesche(String account) {
        try (Keyring keyring = Keyring.create()) {
            keyring.deletePassword(DOMAIN, account);
        } catch (PasswordAccessException e) {
            // Nichts zu loeschen.
        } catch (Exception e) {
            throw new HbciException("Zugangsdaten konnten nicht geloescht werden", e);
        }
    }

    private String pinAccount(long kontoId) {
        return "konto-" + kontoId + "-pin";
    }

    private String passphraseAccount(long kontoId) {
        return "konto-" + kontoId + "-passphrase";
    }
}
