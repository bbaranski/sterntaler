package de.foerderverein.kasse.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/** cellFactory-Helfer fuer Tabellenspalten mit echtem Datums-/Zahlentyp statt vorformatiertem
 * String: die Sortierung von TableColumn arbeitet auf dem Wert aus der cellValueFactory, nicht
 * auf dem angezeigten Text. Ist die Spalte als String typisiert (nur Anzeige-Text), sortiert
 * JavaFX zwangsläufig alphabetisch - z. B. "10.01.2026" vor "2.02.2026". Mit LocalDate/Long als
 * Spaltentyp (beide Comparable) sortiert JavaFX automatisch korrekt chronologisch/numerisch;
 * diese Klasse liefert dafuer nur noch die Anzeige-Formatierung. */
final class SpaltenFormat {

    private SpaltenFormat() {
    }

    static <S> Callback<TableColumn<S, LocalDate>, TableCell<S, LocalDate>> datumZelle(DateTimeFormatter format) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(format));
            }
        };
    }

    static <S> Callback<TableColumn<S, LocalDateTime>, TableCell<S, LocalDateTime>> zeitpunktZelle(DateTimeFormatter format) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(format));
            }
        };
    }

    /** Fuer in Cent gespeicherte Geldbetraege (siehe Buchung.betragCent). */
    static <S> Callback<TableColumn<S, Long>, TableCell<S, Long>> betragCentZelle(NumberFormat waehrungsFormat) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(Long item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : waehrungsFormat.format(BigDecimal.valueOf(item, 2)));
            }
        };
    }
}
