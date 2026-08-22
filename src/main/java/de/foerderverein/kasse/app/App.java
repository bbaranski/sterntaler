package de.foerderverein.kasse.app;

import java.io.IOException;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        Views.init(AppContext.create());
    }

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = Views.loader("/de/foerderverein/kasse/ui/MainView.fxml").load();
        stage.setTitle("Förderverein Kasse");
        stage.setScene(Views.scene(root));
        stage.setWidth(1100);
        stage.setHeight(680);
        stage.setOnCloseRequest(e -> LOG.info("Anwendung wird beendet"));
        stage.show();
    }
}
