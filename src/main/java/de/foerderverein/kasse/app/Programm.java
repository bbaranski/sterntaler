package de.foerderverein.kasse.app;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Programmname und Fassung, beides beim Bauen aus der pom.xml eingesetzt (siehe den
 * gefilterten Ressourcenordner in der pom.xml). Einzige Quelle der Fassungsnummer ist damit
 * {@code <version>} - im Code steht sie nirgends.
 *
 * <p>Bewusst eine Ressource und nicht {@code Implementation-Version} aus dem Jar-Manifest:
 * Beim Start aus dem Entwicklungsstand heraus und in Tests gibt es kein Manifest, der Wert
 * waere dort null. So liefert die Klasse ueberall dasselbe. */
public final class Programm {

    private static final String RESSOURCE = "/de/foerderverein/kasse/app/version.properties";

    public static final String NAME = eigenschaft("name", "Sterntaler");
    public static final String FASSUNG = eigenschaft("fassung", "unbekannt");

    private Programm() {
    }

    private static String eigenschaft(String schluessel, String ersatz) {
        try (InputStream quelle = Programm.class.getResourceAsStream(RESSOURCE)) {
            if (quelle == null) {
                return ersatz;
            }
            Properties werte = new Properties();
            werte.load(new InputStreamReader(quelle, StandardCharsets.UTF_8));
            return werte.getProperty(schluessel, ersatz);
        } catch (IOException e) {
            return ersatz;
        }
    }
}
