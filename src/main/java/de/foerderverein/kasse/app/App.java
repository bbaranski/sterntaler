package de.foerderverein.kasse.app;

import java.io.IOException;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.foerderverein.kasse.ui.DatenbankAuswahlDialog;

public class App extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        Optional<AppContext> context = DatenbankAuswahlDialog.zeigeDialog();
        if (context.isEmpty()) {
            LOG.info("Datenbankauswahl abgebrochen - Anwendung wird beendet");
            Platform.exit();
            System.exit(0);
            return;
        }
        Views.init(context.get());

        Parent root = Views.loader("/de/foerderverein/kasse/ui/MainView.fxml").load();
        stage.setTitle("Sterntaler");
        stage.setScene(Views.scene(root));
        stage.setWidth(1100);
        stage.setHeight(680);
        stage.setOnCloseRequest(e -> LOG.info("Anwendung wird beendet"));
        stage.show();
    }
}
