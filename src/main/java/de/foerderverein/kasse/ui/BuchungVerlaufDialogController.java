package de.foerderverein.kasse.ui;

import java.time.format.DateTimeFormatter;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import de.foerderverein.kasse.app.AppContext;
import de.foerderverein.kasse.domain.AuditEintrag;
import de.foerderverein.kasse.domain.Buchung;

public class BuchungVerlaufDialogController {

    private static final DateTimeFormatter ZEITPUNKT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AppContext ctx;

    @FXML private Label ueberschriftLabel;
    @FXML private TableView<AuditEintrag> verlaufTable;
    @FXML private TableColumn<AuditEintrag, String> zeitpunktColumn;
    @FXML private TableColumn<AuditEintrag, String> aktionColumn;
    @FXML private TableColumn<AuditEintrag, String> benutzerColumn;
    @FXML private TableColumn<AuditEintrag, String> detailsColumn;

    public BuchungVerlaufDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        zeitpunktColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getZeitstempel().format(ZEITPUNKT_FORMAT)));
        aktionColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getAktion().name()));
        benutzerColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBenutzer()));
        detailsColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getDetails()));
    }

    public void setBuchung(Buchung buchung) {
        ueberschriftLabel.setText("Verlauf Buchung vom " + buchung.getBuchungsdatum().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        verlaufTable.setItems(FXCollections.observableArrayList(ctx.buchungService.findVerlauf(buchung.getId())));
    }

    @FXML
    private void onSchliessen() {
        ((Stage) verlaufTable.getScene().getWindow()).close();
    }
}
