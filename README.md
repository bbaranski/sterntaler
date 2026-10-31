# Sterntaler

Sterntaler führt die Kasse eines Schulfördervereins: Einnahmen und Ausgaben eines Kontos, die
Mitglieder samt Beiträgen, und daraus die Berichte für Mitgliederversammlung, Kassenprüfung und
Finanzamt. Das Programm läuft allein auf Ihrem Rechner — ohne Server, ohne Benutzerkonto, ohne
Cloud. Alle Daten liegen in einer Datei, die Sie selbst auswählen.

## Was das Programm kann

- **Buchungen** erfassen, ändern und stornieren — gelöscht wird nie, damit nachvollziehbar
  bleibt, was es gab. Zu jeder Buchung lässt sich ein Beleg als PDF oder JPG hinterlegen.
- **Kontoumsätze abrufen** per HBCI/FinTS, ausschließlich lesend; Überweisungen kann das
  Programm nicht auslösen. Bereits importierte Umsätze erkennt es wieder.
- **Mitglieder verwalten** mit Status, Änderungsverlauf je Mitglied, Import und Export als
  Excel-Datei sowie Serienbriefen aus Word-Vorlagen.
- **Geschäftsjahre** mit frei wählbarem Zeitraum und Startsaldo. Der Abschluss sperrt alle
  Buchungen des Jahres.
- **Fünf Berichte als PDF:** Kassenbericht, Kassenprüfbericht, Mitgliederentwicklung,
  Mitgliederübersicht und Einnahmen-Überschuss-Rechnung.
- **Exporte und Sicherung:** Buchungen und Mitgliederliste als Excel; die Sicherheitskopie legt
  Datenbank, Belege und alle Berichte auf einmal weg — lesbar auch ohne dieses Programm.

## Nutzerhandbuch

Wie Sie das Programm einrichten und im Alltag benutzen, steht im Nutzerhandbuch:
**[docs/handbuch.md](docs/handbuch.md)** — hier im Browser zu lesen, auch als
**[PDF](docs/handbuch.pdf)** zum Weitergeben und Ausdrucken. In der laufenden Anwendung steht
dasselbe Handbuch unter **Hilfe → Handbuch…**.

## Lizenz

Sterntaler steht unter der **MIT-Lizenz** (siehe [LICENSE](LICENSE)), Copyright (c) 2026 Bastian
Baranski. Der englische Text in [LICENSE](LICENSE) ist die rechtlich maßgebliche Fassung.

Das Programm benutzt freie Bibliotheken anderer Urheber. Sie stehen unter ihren eigenen Lizenzen
und sind in [DRITTANBIETER.md](DRITTANBIETER.md) aufgeführt.

**Keine Gewähr:** Die Nutzung erfolgt auf eigene Gefahr. Für die Richtigkeit der erzeugten
Berichte, Salden und Exporte wird keine Gewähr übernommen; die Zahlen sind vor jeder Verwendung
zu prüfen, insbesondere vor einer Kassenprüfung oder einer Abgabe beim Finanzamt. Eine Haftung
für Vorsatz und grobe Fahrlässigkeit sowie nach dem Produkthaftungsgesetz bleibt unberührt.
