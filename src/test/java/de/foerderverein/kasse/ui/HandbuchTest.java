package de.foerderverein.kasse.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Waechter gegen veraltete Dokumentation: jeder Menuepunkt der Anwendung muss im Handbuch
 * vorkommen. Schlaegt fehl, sobald ein Menue hinzukommt oder umbenannt wird, ohne dass
 * docs/handbuch.md nachgezogen wurde - und zwar in demselben Commit, der die Umbenennung bringt.
 * Ohne diesen Test faellt so etwas erst dem Leser auf, der den Menuepunkt nicht findet. */
class HandbuchTest {

    private static final Path HANDBUCH = Path.of("docs", "handbuch.md");
    private static final String MAIN_VIEW = "/de/foerderverein/kasse/ui/MainView.fxml";

    /** Beschriftungen von {@code <Menu text="…">} und {@code <MenuItem text="…">}; andere
     * text-Attribute (Reiter, Spaltenueberschriften) bleiben bewusst aussen vor. */
    private static final Pattern MENUE_BESCHRIFTUNG = Pattern.compile("<Menu(?:Item)?\\s+text=\"([^\"]+)\"");

    @Test
    void jederMenuepunktIstImHandbuchBeschrieben() throws IOException {
        String handbuch = Files.readString(HANDBUCH, StandardCharsets.UTF_8);

        List<String> fehlend = new ArrayList<>();
        for (String beschriftung : menuebeschriftungen()) {
            if (!handbuch.contains(beschriftung)) {
                fehlend.add(beschriftung);
            }
        }

        assertTrue(fehlend.isEmpty(),
                "Diese Menuepunkte kommen in docs/handbuch.md nicht vor: " + fehlend
                        + " - bitte das Handbuch ergaenzen (und docs/handbuch.pdf neu erzeugen, "
                        + "siehe README).");
    }

    /** Stellt sicher, dass der Waechter oben ueberhaupt etwas zu pruefen hat: Greift das Muster
     * eines Tages ins Leere - etwa weil die Menues anders aufgebaut werden -, wuerde er sonst
     * stillschweigend gruen bleiben. */
    @Test
    void menuebeschriftungenWerdenGefunden() throws IOException {
        List<String> beschriftungen = menuebeschriftungen();
        assertTrue(beschriftungen.size() >= 15,
                "Zu wenige Menuebeschriftungen gefunden, Muster passt nicht mehr: " + beschriftungen);
        assertTrue(beschriftungen.contains("Kassenbericht"), beschriftungen.toString());
        assertFalse(beschriftungen.contains("Buchungen"),
                "Reiter sind keine Menuepunkte: " + beschriftungen);
    }

    /** Die Beschriftungen ohne die Auslassungspunkte, mit denen Menuepunkte mit Folgedialog
     * enden - im Fliesstext des Handbuchs stehen die Namen ohne sie. */
    private List<String> menuebeschriftungen() throws IOException {
        String fxml;
        try (InputStream in = getClass().getResourceAsStream(MAIN_VIEW)) {
            if (in == null) {
                throw new IllegalStateException("MainView.fxml nicht im Klassenpfad: " + MAIN_VIEW);
            }
            fxml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<String> beschriftungen = new ArrayList<>();
        Matcher treffer = MENUE_BESCHRIFTUNG.matcher(fxml);
        while (treffer.find()) {
            String text = treffer.group(1).replace("…", "").trim();
            if (!text.isEmpty()) {
                beschriftungen.add(text);
            }
        }
        return beschriftungen;
    }
}
