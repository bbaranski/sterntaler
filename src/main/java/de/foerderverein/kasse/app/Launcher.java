package de.foerderverein.kasse.app;

/** Eigenstaendige Main-Klasse statt direktem Start ueber {@link App#main}: vermeidet, dass die
 * JAR-Manifest-Main-Class selbst von javafx.application.Application erbt. */
public final class Launcher {

    public static void main(String[] args) {
        App.main(args);
    }
}
