package de.foerderverein.kasse.hbci;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;

import org.kapott.hbci.GV_Result.GVRKUms;

import de.foerderverein.kasse.domain.Buchung;

/** Bildet einen von hbci4j gelieferten Umsatz (GVRKUms.UmsLine) auf eine noch nicht gespeicherte
 * Buchung ab und berechnet einen stabilen Hash zur Duplikaterkennung bei erneutem Abruf. */
class UmsatzMapper {

    Buchung toBuchung(GVRKUms.UmsLine line, long kontoId) {
        Buchung buchung = new Buchung();
        buchung.setKontoId(kontoId);
        buchung.setBuchungsdatum(toLocalDate(line.bdate != null ? line.bdate : line.valuta));
        buchung.setWertstellung(toLocalDate(line.valuta));
        buchung.setBetragCent(line.value.getBigDecimalValue()
                .movePointRight(2)
                .longValueExact());
        buchung.setWaehrung(line.value.getCurr());
        buchung.setVerwendungszweck(String.join(" ", nonNull(line.usage)).trim());
        if (line.other != null) {
            buchung.setGegenkontoIban(line.other.iban);
            buchung.setGegenkontoName(line.other.name);
        }
        buchung.setHbciImportHash(hash(line));
        return buchung;
    }

    private LocalDate toLocalDate(java.util.Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private List<String> nonNull(List<String> usage) {
        return usage != null ? usage : List.of();
    }

    private String hash(GVRKUms.UmsLine line) {
        String basis;
        if (line.id != null && !line.id.isBlank()) {
            basis = "id:" + line.id;
        } else if (line.instref != null && !line.instref.isBlank()) {
            basis = "instref:" + line.instref;
        } else {
            basis = "composite:" + line.bdate + "|" + line.valuta + "|" + line.value.getBigDecimalValue()
                    + "|" + String.join(" ", nonNull(line.usage))
                    + "|" + (line.other != null ? line.other.iban : "");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(basis.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 nicht verfuegbar", e);
        }
    }
}
