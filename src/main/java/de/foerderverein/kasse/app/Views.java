package de.foerderverein.kasse.app;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

/** Zentrale Stelle zum Laden von FXML-Ansichten inkl. Verdrahtung des Controllers mit dem
 * AppContext - wird sowohl vom Hauptfenster als auch von allen Dialogen benutzt, damit jeder
 * Controller per Konstruktor Zugriff auf die Services bekommt. */
public final class Views {

    private static AppContext context;

    private Views() {
    }

    static void init(AppContext appContext) {
        context = appContext;
    }

    public static FXMLLoader loader(String fxmlResource) {
        FXMLLoader loader = new FXMLLoader(Views.class.getResource(fxmlResource));
        loader.setControllerFactory(Views::createController);
        return loader;
    }

    /** Scene inkl. gemeinsamem Stylesheet - zentral, damit jedes Dialogfenster gleich aussieht. */
    public static Scene scene(Parent root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(Views.class.getResource("/de/foerderverein/kasse/ui/styles.css").toExternalForm());
        return scene;
    }

    private static Object createController(Class<?> type) {
        try {
            return type.getConstructor(AppContext.class).newInstance(context);
        } catch (NoSuchMethodException e) {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Controller " + type + " konnte nicht erzeugt werden", ex);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Controller " + type + " konnte nicht erzeugt werden", e);
        }
    }
}
