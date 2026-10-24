@echo off
rem Startet Sterntaler auf Windows. Doppelklick genuegt; das Jar muss im selben
rem Ordner liegen. Diese Fassung setzt ein installiertes Java 21 voraus - wer das vermeiden
rem will, nimmt das Installationsprogramm aus demselben Release, das seine eigene Laufzeit
rem mitbringt.
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "JAR=Sterntaler.jar"
if not exist "%JAR%" goto keinjar

rem Erst JAVA_HOME, dann der Suchpfad - Java ist nicht immer im Suchpfad eingetragen.
set "JAVA=java"
set "JAVAW=javaw"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    set "JAVA=%JAVA_HOME%\bin\java"
    set "JAVAW=%JAVA_HOME%\bin\javaw"
)

rem Nicht bloss "where java" pruefen: Es kommt vor, dass ein java.exe im Pfad steht, das sich
rem nicht starten laesst. Erst der Aufruf zeigt, ob wirklich eines da ist.
"%JAVA%" -version >nul 2>&1
if errorlevel 1 goto keinjava

rem Erste Zeile von "java -version" sieht so aus:  openjdk version "21.0.12" 2026-08-18
rem Das dritte Wort ist die Versionsangabe in Anfuehrungszeichen.
for /f "tokens=3" %%v in ('""%JAVA%" -version 2^>^&1 ^| findstr /i "version""') do if not defined ROH set "ROH=%%v"
if not defined ROH goto starten
set "ROH=!ROH:"=!"
for /f "delims=._- tokens=1,2" %%a in ("!ROH!") do (
    set "HAUPT=%%a"
    set "NEBEN=%%b"
)
rem Java bis 8 zaehlt "1.8" statt "8".
if "!HAUPT!"=="1" set "HAUPT=!NEBEN!"
if not defined HAUPT goto starten
if !HAUPT! LSS 21 goto zualt

:starten
rem javaw statt java: sonst bleibt ein leeres Konsolenfenster neben dem Programm stehen.
start "" "%JAVAW%" -Dfile.encoding=UTF-8 -jar "%JAR%"
exit /b 0

:keinjar
echo.
echo   %JAR% wurde nicht gefunden.
echo.
echo   Das Programm und dieses Startskript gehoeren in denselben Ordner.
echo   Bitte das heruntergeladene Archiv vollstaendig entpacken.
echo.
pause
exit /b 1

:keinjava
call :hinweis "Java wurde auf diesem Rechner nicht gefunden."
exit /b 1

:zualt
call :hinweis "Das installierte Java ist zu alt (gefunden: Version !HAUPT!)."
exit /b 1

:hinweis
echo.
echo   %~1
echo.
echo   Diese Fassung des Programms braucht Java 21 oder neuer.
echo   Kostenlos erhaeltlich unter: https://adoptium.net
echo.
echo   Alternative ohne Java: die Datei mit der Endung .exe aus demselben
echo   Download - die bringt alles Noetige mit.
echo.
pause
goto :eof
