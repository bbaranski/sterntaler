package de.foerderverein.kasse.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.Anrede;
import de.foerderverein.kasse.domain.IbanPruefung;
import de.foerderverein.kasse.domain.Mitglied;
import de.foerderverein.kasse.domain.MitgliedStatus;

/** Anlegen/Bearbeiten eines einzelnen Mitglieds als Pop-up-Dialog (analog zu
 * BuchungDialogController) - MitgliederViewController zeigt dafuer keinen eingebetteten
 * Formularbereich mehr an, sondern oeffnet diesen Dialog ueber "Neu"/"Bearbeiten". */
public class MitgliedDialogController {

    private final AppContext ctx;

    private static final DateTimeFormatter DATUM_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML private ComboBox<Anrede> anredeCombo;
    @FXML private TextField vornameField;
    @FXML private TextField nachnameField;
    @FXML private TextField strasseField;
    @FXML private TextField plzField;
    @FXML private TextField ortField;
    @FXML private TextField emailField;
    @FXML private TextField eintrittsdatumField;
    @FXML private TextField austrittsdatumField;
    @FXML private ComboBox<MitgliedStatus> statusCombo;
    @FXML private TextField jahresbeitragField;
    @FXML private TextField ibanField;
    @FXML private Label ibanHinweisLabel;
    @FXML private TextField bicField;
    @FXML private TextField mandatsreferenzField;
    @FXML private TextField sammellastschriftField;
    @FXML private TextArea notizField;
    @FXML private Label fehlerLabel;

    private Mitglied mitglied;
    private boolean gespeichert;

    public MitgliedDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        anredeCombo.setItems(FXCollections.observableArrayList(null, Anrede.HERR, Anrede.FRAU, Anrede.FAMILIE));
        anredeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Anrede anrede) {
                return anredeText(anrede);
            }

            @Override
            public Anrede fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                for (Anrede anrede : Anrede.values()) {
                    if (anredeText(anrede).equals(text)) {
                        return anrede;
                    }
                }
                return null;
            }
        });
        statusCombo.setItems(FXCollections.observableArrayList(MitgliedStatus.values()));
        ibanField.textProperty().addListener((obs, alt, neu) -> aktualisiereIbanHinweis());
    }

    /** bestehend == null bedeutet Neuanlage - dann werden sinnvolle Vorgaben gesetzt (Status
     * Aktiv, Eintrittsdatum heute). */
    public void setDaten(Mitglied bestehend) {
        this.mitglied = bestehend;
        if (bestehend == null) {
            statusCombo.setValue(MitgliedStatus.AKTIV);
            eintrittsdatumField.setText(LocalDate.now().format(DATUM_FORMAT));
            return;
        }
        anredeCombo.setValue(bestehend.getAnrede());
        vornameField.setText(bestehend.getVorname());
        nachnameField.setText(bestehend.getNachname());
        strasseField.setText(bestehend.getStrasse());
        plzField.setText(bestehend.getPlz());
        ortField.setText(bestehend.getOrt());
        emailField.setText(bestehend.getEmail());
        eintrittsdatumField.setText(bestehend.getEintrittsdatum());
        austrittsdatumField.setText(bestehend.getAustrittsdatum());
        statusCombo.setValue(bestehend.getStatus());
        jahresbeitragField.setText(bestehend.getJahresbeitragEuro() != null
                ? bestehend.getJahresbeitragEuro().toPlainString().replace('.', ',') : "");
        ibanField.setText(bestehend.getIban());
        bicField.setText(bestehend.getBic());
        mandatsreferenzField.setText(bestehend.getMandatsreferenz() != null
                ? String.valueOf(bestehend.getMandatsreferenz()) : "");
        sammellastschriftField.setText(bestehend.getSammellastschrift());
        notizField.setText(bestehend.getNotiz());
    }

    public boolean isGespeichert() {
        return gespeichert;
    }

    private static String anredeText(Anrede anrede) {
        if (anrede == null) {
            return "";
        }
        return switch (anrede) {
            case HERR -> "Herr";
            case FRAU -> "Frau";
            case FAMILIE -> "Familie";
        };
    }

    /** Rein informativer Hinweis (siehe IbanPruefung) - blockiert das Speichern bewusst nicht,
     * da manche Sonderfaelle (z. B. vorlaeufig unvollstaendige Angaben) trotzdem gespeichert
     * werden koennen sollen. */
    private void aktualisiereIbanHinweis() {
        String eingabe = ibanField.getText();
        boolean ungueltig = eingabe != null && !eingabe.isBlank() && !IbanPruefung.istGueltig(eingabe);
        ibanHinweisLabel.setText(ungueltig ? "IBAN-Prüfsumme ungültig" : "");
        if (ungueltig) {
            if (!ibanField.getStyleClass().contains("warnung-feld")) {
                ibanField.getStyleClass().add("warnung-feld");
            }
        } else {
            ibanField.getStyleClass().remove("warnung-feld");
        }
    }

    @FXML
    private void onSpeichern() {
        fehlerLabel.setText("");
        try {
            Mitglied ziel = mitglied != null ? mitglied : new Mitglied();
            ziel.setAnrede(anredeCombo.getValue());
            ziel.setVorname(vornameField.getText());
            ziel.setNachname(nachnameField.getText());
            ziel.setStrasse(leerZuNull(strasseField.getText()));
            ziel.setPlz(leerZuNull(plzField.getText()));
            ziel.setOrt(leerZuNull(ortField.getText()));
            ziel.setEmail(leerZuNull(emailField.getText()));
            ziel.setEintrittsdatum(leerZuNull(eintrittsdatumField.getText()));
            ziel.setAustrittsdatum(leerZuNull(austrittsdatumField.getText()));
            ziel.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : MitgliedStatus.AKTIV);
            ziel.setJahresbeitragEuro(parseJahresbeitrag());
            ziel.setIban(leerZuNull(ibanField.getText()));
            ziel.setBic(leerZuNull(bicField.getText()));
            ziel.setMandatsreferenz(parseMandatsreferenz());
            ziel.setSammellastschrift(leerZuNull(sammellastschriftField.getText()));
            ziel.setNotiz(leerZuNull(notizField.getText()));
            ctx.mitgliedService.save(ziel);
            gespeichert = true;
            schliessen();
        } catch (Exception e) {
            fehlerLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void onAbbrechen() {
        schliessen();
    }

    private void schliessen() {
        ((Stage) vornameField.getScene().getWindow()).close();
    }

    private String leerZuNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private Long parseMandatsreferenz() {
        String text = mandatsreferenzField.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mandatsreferenz muss eine Ganzzahl sein");
        }
    }

    private BigDecimal parseJahresbeitrag() {
        String text = jahresbeitragField.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Jahresbeitrag ungueltig, z. B. 24,00");
        }
    }
}
