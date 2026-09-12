package de.foerderverein.kasse.domain;

import java.math.BigInteger;
import java.util.regex.Pattern;

/** Prueft eine IBAN auf Format und Pruefsumme (ISO 7064 MOD-97-10), unabhaengig vom Land - ohne
 * eine Tabelle laenderspezifischer Soll-Laengen zu pflegen. Rein informative Pruefung (siehe
 * V7__mitglied_bankdaten.sql): eine ungueltige IBAN wird in der UI nur als Hinweis angezeigt,
 * das Speichern aber nicht blockiert. */
public final class IbanPruefung {

    private static final Pattern FORMAT = Pattern.compile("[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}");

    private IbanPruefung() {
    }

    public static boolean istGueltig(String iban) {
        if (iban == null) {
            return false;
        }
        String bereinigt = iban.replace(" ", "").toUpperCase();
        if (!FORMAT.matcher(bereinigt).matches()) {
            return false;
        }
        String umgestellt = bereinigt.substring(4) + bereinigt.substring(0, 4);
        StringBuilder numerisch = new StringBuilder();
        for (char zeichen : umgestellt.toCharArray()) {
            if (Character.isDigit(zeichen)) {
                numerisch.append(zeichen);
            } else {
                numerisch.append(zeichen - 'A' + 10);
            }
        }
        return new BigInteger(numerisch.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
