# Sterntaler

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

## Programmversionen zum Weitergeben

Es gibt zwei Wege: die Pakete je Zielsystem, die ihre eigene Java-Laufzeit mitbringen, und ein
plattformuebergreifendes Jar, das ein installiertes Java voraussetzt.

### Pakete mit eigener Laufzeit

Auf dem Zielrechner muss nichts installiert sein.

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

### Eine Fassung fuer Windows und Mac zugleich

```sh
mvn clean package -Pplattformuebergreifend -Djavafx.platform=mac-aarch64
# Ergebnis: target/plattformuebergreifend/Sterntaler.jar
```

Das Jar (~57 MB) startet auf **Windows x64** und auf **Macs ab M1** und setzt dort ein
installiertes **Java 21** voraus. Weitergegeben wird es als ZIP zusammen mit den Dateien aus
[start/](start/) - `start.command`, `start.bat` und `LIESMICH.txt`. Die Skripte suchen Java
(auch ueber `JAVA_HOME` und `/usr/libexec/java_home`), pruefen die Version und melden sonst
verstaendlich, was fehlt. Als ZIP, weil eine einzeln heruntergeladene `.command` ihr
Ausfuehrbar-Bit verliert und sich dann nicht doppelklicken laesst.

Eine mitgelieferte Laufzeit kann es hier nicht geben - die besteht aus nativem Code und gilt
immer nur fuer ein System. Wer ohne Java-Installation auskommen muss, nimmt die Pakete oben.

**Das `-D` gehoert zum Baubefehl.** Die OpenJFX-POMs waehlen den Classifier ihrer transitiven
Artefakte ueber eigene `<os>`-Profile, also nach dem Baurechner. Ohne die Festlegung zieht ein
fremder Laeufer noch einen dritten Plattformsatz herein. Dass beide Zielplattformen ueberhaupt
im Jar landen, sichern die literalen Classifier im Profil - nachpruefbar mit:

```sh
mvn dependency:tree -Pplattformuebergreifend -Djavafx.platform=win
```

Dort muessen `javafx-base` und `javafx-graphics` mit `mac-aarch64` **und** `win` auftauchen.

**Intel-Macs deckt dieses Jar nicht ab.** Deren JavaFX-Natives heissen genauso wie die fuer
Apple Silicon (`libglass.dylib` und weitere) und wuerden einander im Jar ueberschreiben; gegen
Windows kollidieren sie nicht (`glass.dll`). Fuer Intel-Macs gibt es das DMG aus dem Release.

Das Jar landet bewusst in `target/plattformuebergreifend/` und nicht in `target/app/`: Letzteres
ist das Eingabeverzeichnis von `jpackage`, das seinen Inhalt vollstaendig uebernimmt - das Jar
wanderte sonst mit ins DMG.
