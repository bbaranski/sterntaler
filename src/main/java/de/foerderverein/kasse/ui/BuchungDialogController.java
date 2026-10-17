package de.foerderverein.kasse.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.domain.Buchung;
import de.foerderverein.kasse.domain.Kategorie;
import de.foerderverein.kasse.domain.Konto;

public class BuchungDialogController {

    private final AppContext ctx;

    @FXML private ComboBox<Konto> kontoCombo;
    @FXML private DatePicker datumPicker;
    @FXML private TextField kontoauszugField;
    @FXML private TextField betragField;
    @FXML private ComboBox<Kategorie> kategorieCombo;
    @FXML private ComboBox<Aktivitaet> aktivitaetCombo;
    @FXML private TextField verwendungszweckField;
    @FXML private TextField rechnungsnummerField;
    @FXML private TextField gegenkontoNameField;
    @FXML private TextField gegenkontoIbanField;
    @FXML private Label fehlerLabel;

    private Buchung buchung;
    private boolean gespeichert;

    public BuchungDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        KategorieAuswahl.konfiguriere(kategorieCombo);
    }

    public void setDaten(Buchung bestehend, List<Konto> konten, List<Kategorie> kategorien,
                          List<Aktivitaet> aktivitaeten) {
        kontoCombo.setItems(FXCollections.observableArrayList(konten));
        kategorieCombo.setItems(FXCollections.observableArrayList(KategorieAuswahl.alsAuswahlListe(kategorien)));
        aktivitaetCombo.setItems(FXCollections.observableArrayList(
                AktivitaetAuswahl.alsAuswahlListe(aktivitaeten)));

        this.buchung = bestehend;
        if (bestehend != null) {
            konten.stream().filter(k -> k.getId().equals(bestehend.getKontoId())).findFirst()
                    .ifPresent(kontoCombo::setValue);
            kontoCombo.setDisable(true);
            datumPicker.setValue(bestehend.getBuchungsdatum());
            kontoauszugField.setText(bestehend.getKontoauszug());
            betragField.setText(bestehend.getBetragEuro().toPlainString().replace('.', ','));
            kategorien.stream().filter(k -> bestehend.getKategorieId() != null && k.getId().equals(bestehend.getKategorieId()))
                    .findFirst().ifPresent(kategorieCombo::setValue);
            aktivitaeten.stream().filter(a -> bestehend.getAktivitaetId() != null && a.getId().equals(bestehend.getAktivitaetId()))
                    .findFirst().ifPresent(aktivitaetCombo::setValue);
            verwendungszweckField.setText(bestehend.getVerwendungszweck());
            rechnungsnummerField.setText(bestehend.getRechnungsnummer());
            gegenkontoNameField.setText(bestehend.getGegenkontoName());
            gegenkontoIbanField.setText(bestehend.getGegenkontoIban());
        } else {
            datumPicker.setValue(LocalDate.now());
            if (konten.size() == 1) {
                kontoCombo.setValue(konten.get(0));
            }
        }
    }

    public boolean isGespeichert() {
        return gespeichert;
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        Konto konto = kontoCombo.getValue();
        LocalDate datum = datumPicker.getValue();
        BigDecimal betrag;
        try {
            betrag = new BigDecimal(betragField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            fehlerLabel.setText("Betrag ungueltig, z. B. -25,00");
            return;
        }
        if (konto == null) {
            fehlerLabel.setText("Bitte ein Konto waehlen");
            return;
        }
        if (datum == null) {
            fehlerLabel.setText("Bitte ein Datum waehlen");
            return;
        }
        if (betrag.compareTo(BigDecimal.ZERO) == 0) {
            fehlerLabel.setText("Betrag darf nicht 0 sein");
            return;
        }

        try {
            boolean neu = buchung == null;
            Buchung ziel = neu ? new Buchung() : buchung;
            ziel.setKontoId(konto.getId());
            ziel.setBuchungsdatum(datum);
            ziel.setKontoauszug(leerZuNull(kontoauszugField.getText()));
            ziel.setBetragEuro(betrag);
            Kategorie kategorie = kategorieCombo.getValue();
            ziel.setKategorieId(kategorie != null ? kategorie.getId() : null);
            Aktivitaet aktivitaet = aktivitaetCombo.getValue();
            ziel.setAktivitaetId(aktivitaet != null ? aktivitaet.getId() : null);
            ziel.setVerwendungszweck(verwendungszweckField.getText());
            ziel.setRechnungsnummer(leerZuNull(rechnungsnummerField.getText()));
            ziel.setGegenkontoName(gegenkontoNameField.getText());
            ziel.setGegenkontoIban(gegenkontoIbanField.getText());

            if (neu) {
                ctx.buchungService.erfasseManuell(ziel, System.getProperty("user.name"));
            } else {
                ctx.buchungService.aktualisiere(ziel);
            }
            gespeichert = true;
            schliessen();
        } catch (IllegalArgumentException | IllegalStateException e) {
            fehlerLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onAbbrechen() {
        schliessen();
    }

    private void schliessen() {
        ((Stage) betragField.getScene().getWindow()).close();
    }

    private String leerZuNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
