#!/bin/sh
# Startet Sterntaler auf macOS. Doppelklick genuegt; das Jar muss im selben
# Ordner liegen. Diese Fassung setzt ein installiertes Java 21 voraus - wer das vermeiden
# will, nimmt das DMG aus demselben Release, das seine eigene Laufzeit mitbringt.
cd "$(dirname "$0")" || exit 1

JAR=Sterntaler.jar

hinweis() {
    echo ""
    echo "  $1"
    echo ""
    echo "  Diese Fassung des Programms braucht Java 21 oder neuer."
    echo "  Kostenlos erhaeltlich unter: https://adoptium.net"
    echo ""
    echo "  Alternative ohne Java: die Datei mit der Endung .dmg aus demselben"
    echo "  Download - die bringt alles Noetige mit."
    echo ""
    echo "  Zum Schliessen dieses Fensters: Eingabetaste druecken."
    read -r _
    exit 1
}

if [ ! -f "$JAR" ]; then
    echo ""
    echo "  $JAR wurde nicht gefunden."
    echo ""
    echo "  Das Programm und dieses Startskript gehoeren in denselben Ordner."
    echo "  Bitte das heruntergeladene Archiv vollstaendig entpacken."
    echo ""
    echo "  Zum Schliessen dieses Fensters: Eingabetaste druecken."
    read -r _
    exit 1
fi

# Java suchen: erst JAVA_HOME, dann die macOS-eigene Verwaltung. Ohne das scheitert der Start
# auf Rechnern, auf denen Java zwar installiert, aber nicht im Suchpfad ist.
JAVA=java
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
elif [ -x /usr/libexec/java_home ]; then
    heim=$(/usr/libexec/java_home -v 21 2>/dev/null) && [ -n "$heim" ] && JAVA="$heim/bin/java"
fi

# Nicht "command -v java" pruefen: macOS liefert unter /usr/bin/java einen Platzhalter mit,
# den es auch ohne installiertes Java gibt. Erst der Aufruf zeigt, ob wirklich eines da ist.
ausgabe=$("$JAVA" -version 2>&1) || hinweis "Java wurde auf diesem Rechner nicht gefunden."

version=$(echo "$ausgabe" | sed -n 's/^[^"]*"\([0-9][0-9]*\).*/\1/p' | head -1)
# Java bis 8 zaehlt "1.8" statt "8" - sonst stuende im Hinweis "Version 1".
if [ "$version" = "1" ]; then
    version=$(echo "$ausgabe" | sed -n 's/^[^"]*"1\.\([0-9][0-9]*\).*/\1/p' | head -1)
fi

if [ -n "$version" ] && [ "$version" -lt 21 ]; then
    hinweis "Das installierte Java ist zu alt (gefunden: Version $version)."
fi

exec "$JAVA" -Dfile.encoding=UTF-8 -jar "$JAR"
