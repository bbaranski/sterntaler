# Nutzerhandbuch Sterntaler

Stand: Oktober 2026

## Inhalt

1. [Worum es geht](#worum-es-geht)
2. [Programmstart und Datenbankdatei](#programmstart-und-datenbankdatei)
3. [Einrichten vor der ersten Buchung](#einrichten-vor-der-ersten-buchung)
4. [Die Buchungsansicht](#die-buchungsansicht)
5. [Buchungen erfassen, ändern und stornieren](#buchungen-erfassen-ändern-und-stornieren)
6. [Belege zu Buchungen](#belege-zu-buchungen)
7. [Kontoumsätze von der Bank abrufen](#kontoumsätze-von-der-bank-abrufen)
8. [Mitglieder verwalten](#mitglieder-verwalten)
9. [Serienbriefe an Mitglieder](#serienbriefe-an-mitglieder)
10. [Die Berichte](#die-berichte)
11. [Jahresabschluss und Kassenprüfung](#jahresabschluss-und-kassenprüfung)
12. [Datensicherung](#datensicherung)
13. [Wenn etwas nicht klappt](#wenn-etwas-nicht-klappt)
14. [Lizenz und Gewährleistung](#lizenz-und-gewährleistung)

## Worum es geht

„Sterntaler" führt die Kasse eines Schulfördervereins: Einnahmen und Ausgaben eines Kontos, die Mitglieder samt Beiträgen, und daraus die Berichte, die Vorstand, Kassenprüfung und Finanzamt brauchen.

Das Programm läuft allein auf Ihrem Rechner. Es gibt keinen Server und kein Benutzerkonto; alle Daten liegen in einer Datei, die Sie selbst auswählen. Wer diese Datei hat, hat die Kasse — deshalb ist die Datensicherung ein eigener Abschnitt am Ende.

**Für wen dieses Handbuch gedacht ist**

| Rolle | Was Sie hier brauchen |
| --- | --- |
| Kassenwart/-in | Alles. Sie arbeiten täglich mit Buchungen, Mitgliedern und Belegen. |
| Vorstand | Die Berichte und den Jahresabschluss. |
| Kassenprüfer/-innen | Den Kassenprüfbericht und den Ablauf der Prüfung. |

**Wie Sie es lesen**

Die Abschnitte folgen der Reihenfolge, in der Sie dem Programm zum ersten Mal begegnen: einrichten, buchen, verwalten, auswerten, abschließen, sichern. Wenn Sie die Kasse übernehmen und alles schon eingerichtet ist, steigen Sie bei „Die Buchungsansicht" ein.

Im Programm selbst finden Sie dieses Handbuch unter **Hilfe → Handbuch…**, und unter
**Hilfe → Über Sterntaler…** stehen Fassung, Lizenz und die Hinweise zu den mitgelieferten
Fremdbibliotheken.

## Programmstart und Datenbankdatei

**Was Sie brauchen:** Sterntaler läuft auf macOS und Windows und setzt ein installiertes
[Java 21](https://adoptium.net) voraus. Das Programm selbst ist eine einzelne Datei,
`kasse-app.jar`. Ein Doppelklick darauf startet es; weiß Ihr Rechner mit der Datei nichts
anzufangen, hilft der Aufruf `java -jar kasse-app.jar`.

Beim Start fragt das Programm als Erstes, mit welcher Datenbankdatei es arbeiten soll. Erst danach öffnet sich das Hauptfenster.

Im Fenster „Datenbank wählen" steht der Pfad der zuletzt benutzten Datei. Sie haben drei Möglichkeiten:

1. **Starten** — öffnet die angezeigte Datei. Das ist der Normalfall; der Pfad ist beim nächsten Mal schon wieder eingetragen.
2. **Öffnen…** — eine andere vorhandene Datei auswählen, etwa die Kasse eines früheren Jahres oder eine zurückgespielte Sicherung.
3. **Neue Datenbank…** — legt eine leere Kasse an. Das brauchen Sie genau einmal, beim allerersten Mal.

„Beenden" schließt das Programm, ohne etwas zu öffnen.

**Wo Ihre Daten liegen**

Die Datenbankdatei können Sie frei ablegen. Zu ihr gehören zwei Ordner, die das Programm direkt daneben anlegt und nach der Datei benennt:

| Was | Wo | Beispiel bei `kasse.db` |
| --- | --- | --- |
| Buchungen, Mitglieder, Stammdaten | die Datenbankdatei selbst | `kasse.db` |
| Belege (eingescannte Rechnungen) | Ordner neben der Datei | `kasse-belege/` |
| Serienbriefvorlagen | Ordner neben der Datei | `kasse-vorlagen/` |

Dass die Ordner direkt neben der Datei liegen, hat einen Grund: Wenn Sie die Datei verschieben oder kopieren, nehmen Sie die Belege mit, sofern Sie die Ordner mitkopieren. Sichern Sie deshalb nie die `.db`-Datei allein — dafür gibt es die Sicherheitskopie, siehe „Datensicherung".

Voreingestellt ist `kasse.db` im versteckten Ordner `.foerderverein-kasse` in Ihrem Benutzerverzeichnis. Eine Ablage, die Sie selbst regelmäßig sichern, ist die bessere Wahl.

**Die Datenbank im laufenden Betrieb wechseln geht nicht.** Wollen Sie eine andere Kasse öffnen, beenden Sie das Programm und starten es neu.

## Einrichten vor der ersten Buchung

Eine frische Kasse braucht vier Dinge, bevor Sie sinnvoll buchen können. Alle finden Sie im Menü **Einstellungen**, und in dieser Reihenfolge gehen Sie am besten vor.

**1. Vereinsdaten** — Name, Anschrift, Kontakt und Bankverbindung des Vereins. Diese Angaben stehen später als Briefkopf über jedem Bericht. Ohne sie funktionieren die Berichte zwar, haben aber keinen Kopf.

**2. Konto-Einstellungen** — das Vereinskonto mit Bezeichnung, IBAN, BIC und Bankname. Die Bezeichnung ist der Name, unter dem das Konto Ihnen im Programm begegnet, zum Beispiel „Vereinskonto". Hier liegen auch die Zugangsdaten für den Umsatzabruf per Online-Banking; dazu mehr im Abschnitt „Kontoumsätze von der Bank abrufen".

Solange kein Konto angelegt ist, weist die Buchungsansicht beim Öffnen darauf hin. Legen Sie das Konto also zuerst an.

**3. Geschäftsjahre** — der Zeitraum, über den abgerechnet wird. Ein Geschäftsjahr braucht:

| Feld | Bedeutung |
| --- | --- |
| Bezeichnung | Wie es überall angezeigt wird, etwa „2026". |
| Startdatum, Enddatum | Der Zeitraum. Muss nicht das Kalenderjahr sein. |
| Startsaldo (€) | Der Kontostand am Starttag, bevor die erste Buchung des Jahres liegt. |

Der Startsaldo ist die wichtigste Zahl: Auf ihm bauen das laufende Saldo in der Tabelle und der Endsaldo jedes Berichts auf. Er ist der Endsaldo des Vorjahres. Beim allerersten Geschäftsjahr nehmen Sie den Kontostand laut Kontoauszug am Starttag.

**4. Kategorien** — wofür Geld geflossen ist: Mitgliedsbeiträge, Geldspenden, Anschaffungen für die Schule und so weiter. Eine Reihe üblicher Kategorien ist bereits angelegt. Jede Kategorie gehört zu einem steuerlichen Bereich (ideeller Bereich, Vermögensverwaltung, Zweckbetrieb, wirtschaftlicher Geschäftsbetrieb) — diese Zuordnung steuert später allein die Einnahmen-Überschuss-Rechnung. Im Zweifel fragen Sie, wer die Steuererklärung macht.

**Optional: Aktivitäten/Veranstaltungen** — eine zweite, freie Zuordnung quer zu den Kategorien: St. Martin, Sommerfest, Projektwoche. Damit beantworten Sie Fragen wie „Was hat das Sommerfest unterm Strich gebracht?", ohne dafür eigene Kategorien anzulegen. Sie können jederzeit später damit anfangen.

## Die Buchungsansicht

Das Hauptfenster hat zwei Reiter: **Buchungen** und **Mitglieder**. Die Buchungsansicht ist Ihr Arbeitsplatz — eine Tabelle aller Buchungen, darüber eine Leiste mit Schaltflächen, darunter die Zahl der angezeigten Buchungen.

**Die Spalten**

Die Tabelle kann mehr zeigen, als anfangs sichtbar ist. Über **Spalten…** blenden Sie jede Spalte einzeln ein und aus; die Reihenfolge ändern Sie, indem Sie eine Überschrift an ihren neuen Platz ziehen. Beides bleibt nach einem Neustart erhalten.

Drei Spalten verdienen eine Erklärung:

- **Saldo** — der laufende Kontostand nach dieser Buchung, ausgehend vom Startsaldo des Geschäftsjahres. Er ergibt sich aus der Reihenfolge, nicht aus der Buchung selbst.
- **Geschäftsjahr** — welchem Jahr die Buchung zugerechnet wird. Normalerweise ergibt sich das aus dem Buchungsdatum.
- **Quelle** — ob die Buchung von Hand erfasst oder von der Bank abgerufen wurde.

**Sortieren und Suchen**

Ein Klick auf eine Spaltenüberschrift sortiert danach, ein zweiter dreht die Richtung um. Die Sortierung gilt nur für die laufende Sitzung; beim nächsten Start stehen die neuesten Buchungen wieder oben.

Ins Feld **Suchen…** tippen Sie einen beliebigen Text — einen Namen, einen Betrag, eine Rechnungsnummer. Das Programm zählt die Treffer und springt mit den beiden Pfeil-Schaltflächen daneben von einem Treffer zum nächsten. Die Suche filtert die Tabelle nicht, sie markiert.

**Direkt in der Tabelle ändern**

Fünf Spalten können Sie per Doppelklick in der Zeile selbst bearbeiten, ohne den Buchungsdialog zu öffnen:

| Spalte | Wie |
| --- | --- |
| Geschäftsjahr | Auswahl aus den angelegten Jahren |
| Kategorie | Auswahl, nach Bereichen gruppiert |
| Aktivität | Auswahl aus den Veranstaltungen |
| Kontoauszug | Text eintippen |
| Rechnungsnummer | Text eintippen |

Das ist der schnelle Weg, um nach einem Bankabruf einen Stapel Buchungen zu kategorisieren.

**Alle Buchungen als Excel-Datei** bekommen Sie über **Exporte → Kontobewegungen exportieren…** — mit allen Feldern, auch denen, die Sie in der Tabelle ausgeblendet haben, und mit dem Betrag als echter Zahl, sodass sich die Spalte in Excel direkt summieren lässt.

## Buchungen erfassen, ändern und stornieren

**Neue Buchung** öffnet den Erfassungsdialog. Pflicht sind Konto, Datum, Betrag und Kategorie; alles andere ist freiwillig.

Das **Vorzeichen des Betrags** entscheidet über Einnahme oder Ausgabe: `100,00` ist eine Einnahme, `-25,00` eine Ausgabe. Es gibt kein eigenes Feld dafür.

| Feld | Wofür |
| --- | --- |
| Kontoauszug | Nummer des Bankauszugs, auf dem die Buchung steht — die Brücke zum Papier |
| Rechnungsnummer | Ihre Zuordnung zum Beleg |
| Verwendungszweck | Was auf dem Kontoauszug steht |
| Gegenkonto Name, IBAN | Wer gezahlt hat oder an wen gezahlt wurde |

**Ändern** — Zeile auswählen, **Bearbeiten**. Für Kategorie, Aktivität, Geschäftsjahr, Kontoauszug und Rechnungsnummer geht es schneller direkt in der Tabelle (siehe voriger Abschnitt).

**Stornieren statt löschen** — Buchungen werden nie gelöscht. **Stornieren** markiert die Buchung als storniert; sie bleibt sichtbar, wird aber durchgestrichen dargestellt und zählt in keinem Bericht und keinem Saldo mehr mit. So bleibt nachvollziehbar, dass es sie gab — genau das will eine Kassenprüfung sehen.

**Verlauf…** zeigt zu einer ausgewählten Buchung, wer sie wann angelegt, geändert oder storniert hat, einschließlich der alten und neuen Werte.

**Das Geschäftsjahr übersteuern**

Normalerweise gehört eine Buchung in das Geschäftsjahr, in dessen Zeitraum ihr Datum fällt. Die Spalte **Geschäftsjahr** erlaubt eine Ausnahme: Sie können eine Buchung ausdrücklich einem anderen Jahr zuordnen. Das brauchen Sie für Zahlungen über den Jahreswechsel hinweg — etwa eine Rechnung aus dem Dezember, die erst im Januar abgebucht wird und wirtschaftlich noch ins alte Jahr gehört. Die Einstellung **(automatisch)** stellt die Zuordnung nach Datum wieder her.

**Abgeschlossene Jahre sind gesperrt**

Sobald ein Geschäftsjahr abgeschlossen ist, lassen sich seine Buchungen weder ändern noch stornieren; das Programm weist die Änderung mit einem Hinweis ab. Auch das Umhängen funktioniert nicht — weder aus einem abgeschlossenen Jahr heraus noch in eines hinein. Müssen Sie doch noch etwas korrigieren, öffnen Sie das Jahr unter **Einstellungen → Geschäftsjahre…** wieder.

## Belege zu Buchungen

Zu jeder Buchung können Sie genau einen Beleg hinterlegen — die eingescannte Rechnung oder Quittung. Die Spalte **Beleg** zeigt dazu in jeder Zeile die passenden Schaltflächen: **Hochladen…**, wenn noch keiner da ist, sonst **Öffnen**, **Ersetzen…** und **Entfernen**.

**Vorher die Rechnungsnummer eintragen.** Das Programm benennt die Belegdatei nach Geschäftsjahr, Rechnungsnummer und ursprünglichem Dateinamen. Fehlt die Rechnungsnummer, wird der Upload mit einem Hinweis abgelehnt. Tragen Sie sie also zuerst ein — direkt in der Tabelle geht das am schnellsten.

Erlaubt sind **PDF und JPG**. Andere Dateitypen weist das Programm ab.

Die Datei wird in den Belege-Ordner neben der Datenbankdatei kopiert; das Original an seinem ursprünglichen Ort bleibt unangetastet und wird nicht mehr gebraucht. **Öffnen** zeigt den Beleg im Standardprogramm Ihres Rechners. **Entfernen** löscht Datei und Verweis — anders als eine Buchung lässt sich ein Beleg wirklich löschen.

Ändern Sie später die Rechnungsnummer und ersetzen den Beleg, räumt das Programm die alte Datei mit weg, damit im Ordner nichts Verwaistes liegen bleibt.

## Kontoumsätze von der Bank abrufen

Statt jede Buchung abzutippen, holen Sie die Umsätze direkt vom Bankkonto. Das Programm liest dabei nur — es kann keine Überweisungen auslösen.

**Einmalig einrichten:** Unter **Einstellungen → Konto-Einstellungen…** tragen Sie den HBCI/FinTS-Zugang ein: Bankleitzahl, Nutzerkennung (Ihr Online-Banking-Login) und, falls Ihre Bank eine abweichende verlangt, die Kundennummer. Diese Angaben bekommen Sie von Ihrer Bank.

**So rufen Sie ab:**

1. In der Buchungsansicht auf **HBCI-Abruf…**.
2. Zeitraum **Von** und **Bis** wählen.
3. PIN eingeben und **Abrufen**. Auf Wunsch merkt sich der Schlüsselbund Ihres Rechners die PIN für das nächste Mal — gespeichert wird sie dort, nicht im Programm.
4. Das Programm zeigt die gefundenen Umsätze mit Datum, Betrag, Verwendungszweck und Gegenkonto.
5. In der Spalte **Kategorie** ordnen Sie jedem Umsatz eine Kategorie zu, und mit **Übernehmen** wählen Sie aus, was übernommen werden soll.
6. **Importieren** legt die ausgewählten Umsätze als Buchungen an.

**Zweimal abrufen schadet nicht.** Die Spalte **Bereits importiert?** zeigt schon vor dem Import, was Sie längst haben. Das Programm erkennt jeden Umsatz wieder und überspringt ihn — Sie können denselben Zeitraum also gefahrlos mehrfach abrufen.

Übersprungen werden auch Umsätze, die in ein bereits abgeschlossenes Geschäftsjahr fallen. Ein abgeschlossenes Jahr bleibt abgeschlossen, auch beim Import.

Importierte Buchungen tragen in der Spalte **Quelle** den Vermerk `HBCI_IMPORT`. Sie lassen sich danach genauso bearbeiten wie von Hand erfasste.

## Mitglieder verwalten

Der Reiter **Mitglieder** funktioniert wie die Buchungsansicht: eine Tabelle, Spalten über **Spalten…** ein- und ausblendbar, Suche mit den Pfeil-Schaltflächen, Sortierung per Klick auf die Überschrift. Voreingestellt ist die Sortierung nach ID aufsteigend — das ist die Reihenfolge, in der die Mitglieder aufgenommen wurden.

**Anlegen und ändern** — **Neu** öffnet einen leeren Mitgliedsdialog, **Bearbeiten** den der ausgewählten Zeile. Schneller geht es mit einem **Doppelklick auf die Zeile**; das öffnet denselben Dialog.

**Die Felder**

| Feld | Hinweis |
| --- | --- |
| Anrede | Herr, Frau oder Familie — wird in Serienbriefen benutzt |
| Eintritts-, Austrittsdatum | Freier Text, kein Pflichtfeld |
| Status | **Aktiv** oder **Inaktiv** — siehe unten |
| Jahresbeitrag (Euro) | Grundlage der Mitgliederentwicklung |
| IBAN, BIC, Mandatsreferenz, Sammellastschrift | Für den Beitragseinzug per Lastschrift |
| Notiz | Alles, was sonst nirgends hinpasst |

**Aktiv oder inaktiv statt löschen**

Wer austritt, wird auf **Inaktiv** gesetzt, nicht gelöscht. So bleiben die Beiträge vergangener Jahre nachvollziehbar. Die Schaltflächen **Alle**, **Aktive** und **Inaktive** über der Tabelle schalten zwischen den Ansichten um.

**Löschen** gibt es trotzdem — für Fehleingaben. Nach einer Rückfrage ist der Eintrag weg; mehrere markierte Zeilen auf einmal ebenso. Für ausgetretene Mitglieder ist das der falsche Weg.

**Verlauf…** zeigt zu einem ausgewählten Mitglied, wer wann welches Feld geändert hat, mit altem und neuem Wert.

**Mitgliederliste aus Excel übernehmen**

Über **Exporte → Mitgliederliste exportieren…** erzeugen Sie eine Excel-Datei mit allen Mitgliedern und allen Feldern. **Import → Mitgliederliste importieren…** liest eine Datei in genau diesem Format wieder ein — der sichere Weg ist also: erst exportieren, die Datei bearbeiten, dann importieren.

Der Import ist streng: Enthält die Datei auch nur eine fehlerhafte Zeile, wird **nichts** übernommen, und das Programm nennt alle Fehler auf einmal. So entsteht nie ein halb importierter Stand.

## Serienbriefe an Mitglieder

Einladung zur Mitgliederversammlung, Beitragsbestätigung, Rundschreiben: Das Programm füllt eine Word-Vorlage für jedes ausgewählte Mitglied.

**Vorlagen anlegen** — unter **Einstellungen → Serienbriefvorlagen…**. Eine Vorlage besteht aus einem Namen, einem Betreff für den E-Mail-Versand, einer Word-Datei und einem Vorschlagstext samt Signatur (Name und Rolle), den Sie beim Versand noch ändern können.

**Platzhalter in der Word-Datei** schreiben Sie in doppelten geschweiften Klammern, zum Beispiel `{{Vorname}}`. Zur Verfügung stehen alle Mitgliedsfelder:

`{{Anrede}}` `{{Vorname}}` `{{Nachname}}` `{{Straße}}` `{{PLZ}}` `{{Ort}}` `{{Telefon}}` `{{E-Mail}}` `{{Eintrittsdatum}}` `{{Austrittsdatum}}` `{{Status}}` `{{Jahresbeitrag}}` `{{IBAN}}` `{{BIC}}` `{{Mandatsreferenz}}` `{{Sammellastschrift}}` `{{Notiz}}`

Dazu kommen die Vereinsdaten, etwa `{{VereinsName}}`.

**Eine Regel beim Vorlagenbau:** Mitgliederbezogene Platzhalter gehören in den Textkörper des Dokuments, nicht in Kopf- oder Fußzeile. Die Kopfzeile ist in einem zusammengeführten Dokument für alle Briefe dieselbe und kann deshalb nicht je Empfänger wechseln. Vereinsbezogene Platzhalter sind dort unproblematisch.

**Briefe erzeugen**

1. Im Reiter **Mitglieder** die Empfänger markieren — eine Zeile, mehrere oder alle.
2. Auf **Serienbrief…**.
3. Vorlage wählen, Text und Signatur für diesen Versand anpassen.
4. Versandart wählen: **Drucken** erzeugt eine Word-Datei mit allen Briefen hintereinander, **E-Mail** eine Nachricht mit allen Empfängern im BCC — niemand sieht die Adressen der anderen.
5. **Erzeugen**.

## Die Berichte

Alle Berichte entstehen als PDF. Sie wählen einen Speicherort, danach öffnet das Programm die fertige Datei. Jede Seite trägt unten „Seite x von y", jeder Bericht oben den Briefkopf aus den Vereinsdaten.

Für die ersten beiden Berichte fragt das Programm vorher nach Konto und Geschäftsjahr; gibt es nur ein Konto, entfällt die Frage.

| Bericht | Was drinsteht | Wann Sie ihn brauchen |
| --- | --- | --- |
| **Kassenbericht…** | Einnahmen und Ausgaben je Kategorie, Startsaldo, Saldo des Zeitraums, Endsaldo; Unterschriftenzeile | Der Jahresbericht für die Mitgliederversammlung |
| **Kassenprüfbericht…** | Jede einzelne Buchung des Jahres mit laufendem Saldo, dazu Entlastungsempfehlung und Unterschriftenzeile | Für die Kassenprüfung — der Beleg, dass der Kassenbericht stimmt |
| **Mitgliederentwicklung…** | Kennzahlen der aktiven Mitglieder: Anzahl und Jahresbeiträge (Summe, kleinster, größter, Durchschnitt) | Zahlen für den Rechenschaftsbericht |
| **Mitgliederübersicht…** | Alle aktiven Mitglieder mit ihren Stammdaten, nach ID sortiert | Die Liste zum Nachschlagen und Abhaken |
| **Einnahmen-Überschuss-Rechnung…** | Einnahmen und Ausgaben nach den vier steuerlichen Bereichen | Für die Steuererklärung des Vereins |

**Was Sie über einzelne Berichte wissen sollten**

**Kassenbericht und Kassenprüfbericht** werten dieselben Buchungen aus und kommen zwangsläufig auf denselben Endsaldo. Weicht etwas ab, liegt es nicht an den Berichten. Beide lassen stornierte Buchungen und Buchungen ohne Betrag außen vor.

**Der Kassenprüfbericht** enthält den Satz „Die Kassenprüfung hat keine Beanstandungen ergeben. Die Kassenprüfer/-innen empfehlen der Mitgliederversammlung die Entlastung des Vorstands." Das ist eine Vorlage zum Unterschreiben — lesen Sie ihn vor der Unterschrift, und gab es Beanstandungen, gehört das in einen gesonderten Vermerk.

**Die Einnahmen-Überschuss-Rechnung** betrachtet immer ein Kalenderjahr, das Sie beim Erstellen eingeben, nicht das Geschäftsjahr. Sie gliedert nach ideellem Bereich, Vermögensverwaltung, Zweckbetrieb und wirtschaftlichem Geschäftsbetrieb — diese Zuordnung kommt allein aus dem Bereich der jeweiligen Kategorie. Stimmt dort etwas nicht, korrigieren Sie es unter **Einstellungen → Kategorien…**, nicht an den Buchungen.

**Mitgliederentwicklung und Mitgliederübersicht** zählen nur aktive Mitglieder und brauchen deshalb weder Konto noch Geschäftsjahr. Sie zeigen immer den heutigen Stand.

## Jahresabschluss und Kassenprüfung

So geht ein Geschäftsjahr zu Ende — von der letzten Buchung bis zur Entlastung des Vorstands.

1. **Vollständigkeit prüfen.** Alle Umsätze bis zum Jahresende abrufen oder erfassen. Den Endsaldo in der Spalte **Saldo** mit dem letzten Kontoauszug des Jahres vergleichen. Stimmen beide überein, sind Sie vollständig.
2. **Zuordnungen nachziehen.** Jede Buchung braucht eine Kategorie. Zahlungen über den Jahreswechsel gegebenenfalls über die Spalte **Geschäftsjahr** dem richtigen Jahr zuordnen.
3. **Belege vervollständigen.** Jede Ausgabe braucht ihren Beleg — danach fragt die Kassenprüfung.
4. **Berichte erzeugen.** Kassenbericht und Kassenprüfbericht ausdrucken. Der Endsaldo muss in beiden gleich sein und zum Kontoauszug passen.
5. **Prüfung durchführen.** Die Kassenprüfer/-innen gehen den Kassenprüfbericht gegen Kontoauszüge und Belege durch und unterschreiben beide Berichte.
6. **Jahr abschließen.** Unter **Einstellungen → Geschäftsjahre…** das Jahr markieren, unter „Ausgewähltes Geschäftsjahr abschließen" das **Prüfungsdatum** und die **Kassenprüfer/-in** eintragen, dann **Abschließen**.
7. **Sicherung anlegen.** Eine Sicherheitskopie des abgeschlossenen Jahres aufbewahren, siehe nächster Abschnitt.
8. **Nächstes Jahr anlegen** — mit dem Endsaldo des abgeschlossenen Jahres als **Startsaldo**. Das ist der Punkt, an dem die Jahre ineinandergreifen.

**Was der Abschluss bewirkt**

Alle Buchungen des Jahres sind gesperrt: kein Ändern, kein Stornieren, kein Umhängen, und der Bankabruf überspringt Umsätze, die in dieses Jahr fallen. Der gedruckte Kassenbericht und die Datenlage können so nicht mehr auseinanderlaufen.

**Wieder öffnen** hebt die Sperre auf, falls doch noch etwas zu korrigieren ist. Dann gilt: korrigieren, Berichte neu erzeugen, erneut abschließen. Ist der alte Bericht schon unterschrieben, sprechen Sie die Korrektur mit der Kassenprüfung ab.

## Datensicherung

Es gibt keinen Server, der im Hintergrund für Sie sichert. Geht Ihr Rechner verloren, ist die Kasse weg — es sei denn, Sie haben eine Sicherheitskopie.

**Exporte → Sicherheitskopie…** fragt nach einem Zielordner und legt darin einen neuen Unterordner an, benannt nach Datum und Uhrzeit. Eine bestehende Sicherung wird also nie überschrieben.

Darin liegen fünf Dinge:

| Inhalt | Form |
| --- | --- |
| Die vollständige Kasse | Kopie der Datenbankdatei |
| Alle Buchungen | `Kontobewegungen.xlsx` |
| Alle Mitglieder | `Mitgliederliste.xlsx` |
| Alle Berichte | Ordner `Berichte` |
| Alle Belege | Ordner `Belege` |

Im Ordner `Berichte` liegt alles, was sich aus dem Datenbestand erzeugen lässt: je Geschäftsjahr ein Kassenbericht und ein Kassenprüfbericht, je Kalenderjahr mit Buchungen eine Einnahmen-Überschuss-Rechnung, dazu Mitgliederentwicklung und Mitgliederübersicht. Bei mehreren Konten steht das Konto im Dateinamen.

Die Excel-Dateien und die PDF-Berichte sind der Grund, warum diese Sicherung mehr wert ist als eine Dateikopie: Sie können auch ohne dieses Programm gelesen werden, in zehn Jahren und auf jedem Rechner.

**Wann Sie sichern sollten:** nach jedem Jahresabschluss (unbedingt), nach jedem größeren Bankabruf, und bevor Sie etwas Größeres umstellen. Legen Sie die Sicherung auf ein anderes Medium als die Arbeitsdatei — ein externes Laufwerk oder einen Cloud-Ordner.

**Wiederherstellen** — Kopieren Sie die Datenbankdatei aus der Sicherung an Ihren Arbeitsort und den Ordner `Belege` dorthin, wo das Programm die Belege erwartet: in den Ordner neben der Datenbankdatei, benannt nach ihr (bei `kasse.db` also `kasse-belege`). Dann starten Sie das Programm und wählen im Startfenster über **Öffnen…** diese Datei.

**Beim Übergeben der Kasse** an eine Nachfolgerin oder einen Nachfolger geben Sie eine vollständige Sicherheitskopie weiter, nicht nur die `.db`-Datei — sonst fehlen die Belege.

## Wenn etwas nicht klappt

| Situation | Woran es liegt | Was zu tun ist |
| --- | --- | --- |
| „Dieses Geschäftsjahr ist abgeschlossen und kann nicht mehr geändert werden." | Die Buchung liegt in einem abgeschlossenen Jahr | Jahr unter **Einstellungen → Geschäftsjahre…** wieder öffnen, korrigieren, erneut abschließen |
| Beleg lässt sich nicht hochladen | Die Buchung hat noch keine Rechnungsnummer | Rechnungsnummer eintragen, dann erneut hochladen |
| Hinweis, es sei noch kein Konto angelegt | Die Kasse ist frisch | **Einstellungen → Konto-Einstellungen…** |
| Der Endsaldo passt nicht zum Kontoauszug | Buchungen fehlen, sind doppelt oder hängen im falschen Jahr | Kassenprüfbericht erzeugen und Zeile für Zeile gegen den Auszug prüfen |
| Saldo-Spalte wirkt falsch | Der Startsaldo des Geschäftsjahres stimmt nicht | Startsaldo prüfen — er muss der Endsaldo des Vorjahres sein |
| Bankabruf bringt nichts | Zeitraum, Zugangsdaten oder PIN | Zeitraum prüfen; Zugangsdaten in den Konto-Einstellungen gegen die Angaben der Bank halten |
| Ein Umsatz fehlt nach dem Abruf | Er war schon da, oder sein Jahr ist abgeschlossen | Spalte **Bereits importiert?** im Abrufdialog ansehen |
| Mitgliederimport bricht komplett ab | Mindestens eine Zeile ist fehlerhaft | Das Programm listet alle Fehler auf; Datei korrigieren und erneut importieren — es wurde nichts übernommen |
| Ein Bericht hat keinen Briefkopf | Vereinsdaten fehlen | **Einstellungen → Vereinsdaten…** ausfüllen |
| Beträge in der EÜR stehen im falschen Bereich | Der Bereich der Kategorie stimmt nicht | **Einstellungen → Kategorien…** korrigieren, Bericht neu erzeugen |
| Eine Buchung soll weg | Löschen ist nicht vorgesehen | **Stornieren** — sie bleibt sichtbar, zählt aber nirgends mehr mit |
| Falsche Kasse geöffnet | Die Datenbank lässt sich im Betrieb nicht wechseln | Programm beenden, neu starten, im Startfenster **Öffnen…** |

**Wer wann was geändert hat**, beantwortet in beiden Ansichten die Schaltfläche **Verlauf…** — für die ausgewählte Buchung oder das ausgewählte Mitglied, mit altem und neuem Wert.

Bevor Sie etwas Größeres reparieren, legen Sie eine Sicherheitskopie an. Dann können Sie in Ruhe probieren.

## Lizenz und Gewährleistung

Sterntaler ist freie Software und steht unter der MIT-Lizenz. Sie dürfen das Programm nutzen,
weitergeben und verändern; der Quellcode ist offen. Den vollständigen Lizenztext finden Sie in
der Datei `LICENSE`, die jeder Auslieferung beiliegt.

Das Programm bringt freie Bibliotheken anderer Urheber mit. Deren Lizenzen sind in der Datei
`DRITTANBIETER.md` aufgeführt; im Programm erreichen Sie beides über **Hilfe → Über
Sterntaler…**.

**Gewährleistung:** Die Nutzung erfolgt auf eigene Gefahr. Für die Richtigkeit der erzeugten
Berichte, Salden und Exporte wird keine Gewähr übernommen — prüfen Sie die Zahlen vor jeder
Verwendung, insbesondere vor einer Kassenprüfung oder einer Abgabe beim Finanzamt. Eine
Haftung für Vorsatz und grobe Fahrlässigkeit sowie nach dem Produkthaftungsgesetz bleibt
unberührt.
