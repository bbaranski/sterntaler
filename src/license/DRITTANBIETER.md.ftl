<#-- Bewusst mit <#if> statt dem Bedingungsoperator: In FreeMarker kollidiert "? :" mit
     der Syntax fuer eingebaute Funktionen. -->
<#function artefakt p>
  <#local koordinaten = " (" + p.groupId + ":" + p.artifactId + ":" + p.version + ")">
  <#if p.name?starts_with("Unnamed")>
    <#return p.artifactId + koordinaten>
  </#if>
  <#return p.name + koordinaten>
</#function>
# Hinweise zu Fremdbibliotheken

Sterntaler selbst steht unter der MIT-Lizenz (siehe `LICENSE`). Das Programm bringt die
folgenden freien Bibliotheken mit; sie stehen unter den hier genannten Lizenzen und bleiben
Eigentum ihrer jeweiligen Urheber.

Einige Bibliotheken sind **doppelt lizenziert** und erscheinen deshalb unter mehreren
Überschriften. Sterntaler stützt sich dabei jeweils auf die freizügigere Wahl:
openpdf auf die **MPL 2.0**, logback auf die **EPL 2.0**, JNA auf die **Apache-Lizenz 2.0**.
Als Bibliothek unter der LGPL bleibt damit allein hbci4j.

Nicht aufgeführt, weil keine Maven-Abhängigkeiten: die in den Installationspaketen
mitgelieferte **Java-Laufzeit** (GPLv2 mit Classpath Exception; die Lizenztexte liegen im
Paket unter `runtime/.../legal/`) und die Schrift **Liberation Sans** (SIL Open Font License
1.1), die im Handbuch-PDF eingebettet ist. Siehe dazu den Abschnitt „Lizenz" in der README.

Diese Datei wird erzeugt und nicht von Hand gepflegt:

```sh
mvn -o license:add-third-party@drittanbieter
```
<#list licenseMap as eintrag>
  <#assign lizenz = eintrag.getKey()/>
  <#assign projekte = eintrag.getValue()/>
  <#if (projekte?size > 0)>

## ${lizenz}
<#list projekte as p>
- ${artefakt(p)}
</#list>
  </#if>
</#list>
