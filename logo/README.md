# Logo

Angelehnt an „Die Sterntaler" der Brüder Grimm: Das Mädchen hält seine Schürze auf, und die
Sterne fallen als Taler hinein. Dieselbe Marke dient als Logo und als Programmicon.

## Zwei Zeichnungen derselben Marke

| Zeichnung | Verwendung |
| --- | --- |
| **`sterntaler-logo.svg`** — Schürze, Taler, Sternenregen | Webseite, Startbild, Briefkopf, Programmicon ab 128 px |
| **`sterntaler-logo-kompakt.svg`** — dieselbe Schürze ohne Sternenregen, dafür größer im Bild | Programmicon bis 64 px |

Der Grund für die zweite Zeichnung ist nachgemessen: In 32 Pixeln sind die fallenden Sterne
nur noch Rauschen, und das Tuch braucht die ganze Fläche, damit der Stern im Taler überhaupt
stehen bleibt. Icon-Dateien nehmen je Größe ein eigenes Bild auf — genau dafür ist das da.

## Dateien

Maßgeblich sind die SVG; alles andere ist daraus gerendert.

```
sterntaler-logo.svg                  Schürze mit Taler und Sternenregen, dunkler Grund
sterntaler-logo-transparent.svg      dasselbe ohne Grund (nur für dunkle Flächen)
sterntaler-logo-kompakt.svg          Fassung für kleine Icon-Größen

sterntaler-logo-{512,1024}.png
sterntaler-logo-transparent-{512,1024}.png
sterntaler-icon-{16,32,48,64,128,256,512,1024}.png   gerenderte Icon-Größen

sterntaler.icns                      macOS, zehn Größen
sterntaler.ico                       Windows, sechs Größen (16 bis 256 px)
```

**Zur transparenten Fassung:** Schürze und Taler sind hell. Auf weißem Grund verschwinden sie
— dafür ist die Fassung mit dunklem Grund da, die auf hellen wie dunklen Seiten trägt.

## Farben

| | Farbwert | Verwendung |
| --- | --- | --- |
| Nachtblau | `#16233F` → `#0E1729` | Grund, Saum um den Taler |
| Gold | `#FFD970` → `#F0B429` | Taler, fallende Sterne |
| Gold dunkel | `#B9790C` | Prägerand des Talers |
| Creme | `#F3E7D3` | Schürzentuch, Stern im Taler |

## Als Programmicon

Eingebunden über den Parameter `icon` in den drei jpackage-Profilen der `pom.xml`: `.icns` für
die beiden macOS-Profile, `.ico` für Windows. Nach einer Änderung an den SVG müssen die
PNG-Größen und daraus beide Icon-Dateien neu erzeugt werden.

Die `.ico` entsteht ohne Bildwerkzeug: Seit Windows Vista darf ein ICO-Eintrag eine
PNG-Datei unverändert enthalten, die Datei ist dann nur ein Verzeichnis davor.
