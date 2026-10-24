# Förderverein Kasse

Kassenführung für einen Schulförderverein: Buchungen mit Belegen, Mitgliederverwaltung,
Umsatzabruf per HBCI/FinTS und die Berichte für Mitgliederversammlung, Kassenprüfung und
Finanzamt. JavaFX-Anwendung mit SQLite-Datenbank, läuft ohne Server allein auf dem Rechner
der Kassenwartin oder des Kassenwarts.

## Nutzerhandbuch

**[docs/handbuch.md](docs/handbuch.md)** — hier im Browser zu lesen, auch als
**[PDF](docs/handbuch.pdf)** zum Weitergeben und Ausdrucken. In der laufenden Anwendung steht
dasselbe Handbuch unter **Hilfe → Handbuch…**.

## Entwicklung

```sh
mvn -o clean test     # bauen und testen
mvn -o clean compile  # nur bauen
mvn -o clean package  # Uber-Jar unter target/app/kasse-app.jar
```

Voraussetzung ist ein JDK 21. Startklasse ist `de.foerderverein.kasse.app.Launcher`.

## Programmversionen zum Weitergeben

Die fertigen Pakete bringen ihre eigene Java-Laufzeit mit; auf dem Zielrechner muss nichts
installiert sein.

```sh
mvn -o clean package      # muss vorher laufen
mvn -o jpackage:jpackage  # Ergebnis unter target/dist/
```

Maven waehlt das Profil anhand des Baurechners: `mac-aarch64` und `mac-x86_64` liefern ein DMG,
`windows` ein EXE-Installationsprogramm.

**`jpackage` kann nicht quer uebersetzen.** Ein Windows-Paket entsteht nur auf Windows, ein
macOS-Paket nur auf macOS, und ein Mac mit Apple Silicon baut kein Intel-Paket. Fuer alle
Zielsysteme auf einmal gibt es den Workflow
[Programmpakete](.github/workflows/pakete.yml): unter *Actions* starten, er baut auf drei
Laeufern und haengt die Dateien bei einem Tag `v*` an ein Release.

Zwei Dinge, die auf dem Zielrechner auffallen:

- Die Pakete sind **nicht signiert**. macOS meldet beim ersten Start einen unbekannten
  Entwickler (oeffnen ueber Rechtsklick -> *Oeffnen*), Windows zeigt den SmartScreen-Hinweis.
  Dagegen hilft nur ein kostenpflichtiges Entwicklerzertifikat.
- Unter Windows setzen die Typen `EXE` und `MSI` das **WiX Toolset** auf dem Baurechner voraus.
  Fehlt es, liefert `mvn jpackage:jpackage -Djpackage.type=APP_IMAGE` stattdessen ein
  startbares Programmverzeichnis ohne Installationsprogramm.

Welche JDK-Module in die mitgelieferte Laufzeit wandern, steht in den Profilen der `pom.xml`.
Die Liste stammt aus `jdeps` und ist nicht beliebig kuerzbar: Ohne `jdk.unsupported` etwa
startet die Anwendung zwar, kann ihr Fenster aber nicht zeichnen.

### Handbuch ändern

Quelle ist allein `docs/handbuch.md`. Nach jeder Änderung daran das PDF neu erzeugen und
beides zusammen einchecken:

```sh
mvn -o compile exec:java@handbuch
```

Das schreibt `docs/handbuch.pdf` und die Prüfsumme `docs/handbuch.md.sha256`. Bewusst kein
Schritt des normalen Builds: Ein PDF trägt einen Erzeugungszeitpunkt und wäre sonst nach jedem
Build geändert, ohne dass sich am Text etwas getan hätte.

Zwei Tests halten Handbuch und Programm zusammen:

| Test | Schlägt fehl, wenn |
| --- | --- |
| `HandbuchTest` | ein Menüpunkt hinzukommt oder umbenannt wird, ohne dass das Handbuch es erwähnt |
| `HandbuchPdfTest` | `docs/handbuch.md` geändert wurde, ohne das PDF neu zu erzeugen |

Die Abhängigkeiten für die PDF-Erzeugung (flexmark, openhtmltopdf) stehen im Geltungsbereich
`provided` und landen deshalb nicht im ausgelieferten Jar.
