# Hinweise zu Fremdbibliotheken

Sterntaler selbst steht unter der MIT-Lizenz (siehe `LICENSE`). Das Programm bringt die
folgenden freien Bibliotheken mit; sie stehen unter den hier genannten Lizenzen und bleiben
Eigentum ihrer jeweiligen Urheber.

Einige Bibliotheken sind **doppelt lizenziert** und erscheinen deshalb unter mehreren
Überschriften. Sterntaler stützt sich dabei jeweils auf die freizügigere Wahl:
openpdf auf die **MPL 2.0**, logback auf die **EPL 2.0**, JNA auf die **Apache-Lizenz 2.0**.
Als Bibliothek unter der LGPL bleibt damit allein hbci4j.

Nicht aufgeführt, weil keine Maven-Abhängigkeit: die Schrift **Liberation Sans**
(SIL Open Font License 1.1), die im Handbuch-PDF eingebettet ist. Siehe dazu den Abschnitt
„Lizenz" in der README.

Diese Datei wird erzeugt und nicht von Hand gepflegt:

```sh
mvn -o license:add-third-party@drittanbieter
```

## Apache License, Version 2.0
- HKDF-RFC5869 (at.favre.lib:hkdf:1.1.0)
- Brotli4j (com.aayushatharva.brotli4j:brotli4j:1.23.0)
- Brotli4j (com.aayushatharva.brotli4j:native-osx-aarch64:1.23.0)
- Brotli4j (com.aayushatharva.brotli4j:service:1.23.0)
- Jackson-annotations (com.fasterxml.jackson.core:jackson-annotations:2.21)
- SparseBitSet (com.zaxxer:SparseBitSet:1.3)
- Apache Commons Codec (commons-codec:commons-codec:1.20.0)
- Apache Commons IO (commons-io:commons-io:2.21.0)
- Java Native Access (net.java.dev.jna:jna:5.13.0)
- Java Native Access Platform (net.java.dev.jna:jna-platform:5.13.0)
- Apache Commons Collections (org.apache.commons:commons-collections4:4.5.0)
- Apache Commons Compress (org.apache.commons:commons-compress:1.28.0)
- Apache Commons Lang (org.apache.commons:commons-lang3:3.18.0)
- Apache Commons Math (org.apache.commons:commons-math3:3.6.1)
- Apache Log4j API (org.apache.logging.log4j:log4j-api:2.24.3)
- Log4j API to SLF4J Adapter (org.apache.logging.log4j:log4j-to-slf4j:2.24.3)
- Apache POI - Common (org.apache.poi:poi:5.5.1)
- Apache POI - API based on OPC and OOXML schemas (org.apache.poi:poi-ooxml:5.5.1)
- Apache POI (org.apache.poi:poi-ooxml-lite:5.5.1)
- XmlBeans (org.apache.xmlbeans:xmlbeans:5.3.0)
- flyway-core (org.flywaydb:flyway-core:13.3.0)
- flyway-database-nc-sqlite (org.flywaydb:flyway-database-nc-sqlite:13.3.0)
- flyway-nc-core (org.flywaydb:flyway-nc-core:13.3.0)
- SQLite JDBC (org.xerial:sqlite-jdbc:3.53.2.1)
- Jackson-core (tools.jackson.core:jackson-core:3.1.5)
- jackson-databind (tools.jackson.core:jackson-databind:3.1.5)

## BSD 2-Clause License
- jkeychain (pt.davidafsilva.apple:jkeychain:1.1.0)

## BSD 3-Clause License
- java-keyring (com.github.javakeyring:java-keyring:1.0.4)
- curvesapi (com.github.virtuald:curvesapi:1.08)

## EPL-2.0
- Logback Classic Module (ch.qos.logback:logback-classic:1.5.38)
- Logback Core Module (ch.qos.logback:logback-core:1.5.38)

## Eclipse Distribution License 1.0
- istack common utility code runtime (com.sun.istack:istack-commons-runtime:4.1.2)
- Jakarta Activation API (jakarta.activation:jakarta.activation-api:2.1.3)
- Jakarta XML Binding API (jakarta.xml.bind:jakarta.xml.bind-api:4.0.2)
- Angus Activation Registries (org.eclipse.angus:angus-activation:2.0.2)
- JAXB Core (org.glassfish.jaxb:jaxb-core:4.0.5)
- JAXB Runtime (org.glassfish.jaxb:jaxb-runtime:4.0.5)
- TXW2 Runtime (org.glassfish.jaxb:txw2:4.0.5)

## GNU Lesser General Public License, Version 2.1
- Logback Classic Module (ch.qos.logback:logback-classic:1.5.38)
- Logback Core Module (ch.qos.logback:logback-core:1.5.38)
- HBCI4j (com.github.hbci4j:hbci4j-core:4.1.12)
- OpenPDF core modern (com.github.librepdf:openpdf:3.0.5)
- Java Native Access (net.java.dev.jna:jna:5.13.0)
- Java Native Access Platform (net.java.dev.jna:jna-platform:5.13.0)

## GPLv2+CE
- javafx base (org.openjfx:javafx-base:21.0.12)
- javafx controls (org.openjfx:javafx-controls:21.0.12)
- javafx fxml (org.openjfx:javafx-fxml:21.0.12)
- javafx graphics (org.openjfx:javafx-graphics:21.0.12)

## MIT License
- dbus-java-core (com.github.hypfvieh:dbus-java-core:4.2.1)
- dbus-java-transport-native-unixsocket (com.github.hypfvieh:dbus-java-transport-native-unixsocket:4.2.1)
- secret-service (de.swiesend:secret-service:1.8.1-jdk17)
- SLF4J API Module (org.slf4j:slf4j-api:2.0.18)

## Mozilla Public License Version 2.0
- OpenPDF core modern (com.github.librepdf:openpdf:3.0.5)
