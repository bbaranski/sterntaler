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
import de.foerderverein.kasse.domain.Mitglied;

public class MitgliedVerlaufDialogController {

    private static final DateTimeFormatter ZEITPUNKT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AppContext ctx;

    @FXML private Label ueberschriftLabel;
    @FXML private TableView<AuditEintrag> verlaufTable;
    @FXML private TableColumn<AuditEintrag, String> zeitpunktColumn;
    @FXML private TableColumn<AuditEintrag, String> aktionColumn;
    @FXML private TableColumn<AuditEintrag, String> benutzerColumn;
    @FXML private TableColumn<AuditEintrag, String> feldColumn;
    @FXML private TableColumn<AuditEintrag, String> wertAltColumn;
    @FXML private TableColumn<AuditEintrag, String> wertNeuColumn;
    @FXML private TableColumn<AuditEintrag, String> detailsColumn;

    public MitgliedVerlaufDialogController(AppContext ctx) {
        this.ctx = ctx;
    }

    @FXML
    private void initialize() {
        zeitpunktColumn.setCellValueFactory(cd ->
                new ReadOnlyStringWrapper(cd.getValue().getZeitstempel().format(ZEITPUNKT_FORMAT)));
        aktionColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getAktion().name()));
        benutzerColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getBenutzer()));
        feldColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getFeld()));
        wertAltColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getWertAlt()));
        wertNeuColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getWertNeu()));
        detailsColumn.setCellValueFactory(cd -> new ReadOnlyStringWrapper(cd.getValue().getDetails()));
    }

    public void setMitglied(Mitglied mitglied) {
        ueberschriftLabel.setText("Verlauf " + mitglied.getVollerName());
        verlaufTable.setItems(FXCollections.observableArrayList(ctx.mitgliedService.findVerlauf(mitglied.getId())));
    }

    @FXML
    private void onSchliessen() {
        ((Stage) verlaufTable.getScene().getWindow()).close();
    }
}
