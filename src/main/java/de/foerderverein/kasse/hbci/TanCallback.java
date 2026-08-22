package de.foerderverein.kasse.hbci;

/** Wird nur in Ausnahmefaellen benoetigt: Manche Banken verlangen laut PSD2 auch bei reinem
 * Lesezugriff (Umsatzabruf) in regelmaessigen Abstaenden eine erneute Starke Kundenauthentifizierung
 * per TAN. Die Implementierung muss den Aufrufer (HBCI-Hintergrundthread) blockieren, bis der
 * Nutzer die TAN eingegeben hat, und darf daher nicht auf dem JavaFX-Application-Thread laufen. */
@FunctionalInterface
public interface TanCallback {

    String promptTan(String hinweis);
}
