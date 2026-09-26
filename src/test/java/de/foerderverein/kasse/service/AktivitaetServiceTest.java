package de.foerderverein.kasse.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.foerderverein.kasse.domain.Aktivitaet;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.TestDatabase;

class AktivitaetServiceTest {

    @Test
    void nameFehltWirdAbgelehnt() {
        AktivitaetService service = new AktivitaetService(new AktivitaetRepository(TestDatabase.create()));
        Aktivitaet aktivitaet = new Aktivitaet();

        assertThrows(IllegalArgumentException.class, () -> service.save(aktivitaet));
    }

    @Test
    void bereichFehltWirdAbgelehnt() {
        AktivitaetService service = new AktivitaetService(new AktivitaetRepository(TestDatabase.create()));
        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setName("Sankt Martin");
        aktivitaet.setBereich(null);

        assertThrows(IllegalArgumentException.class, () -> service.save(aktivitaet));
    }

    @Test
    void deaktiviereSetztAktivFalse() {
        AktivitaetService service = new AktivitaetService(new AktivitaetRepository(TestDatabase.create()));
        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setName("Sankt Martin");
        service.save(aktivitaet);

        service.deaktiviere(aktivitaet);

        assertFalse(aktivitaet.isAktiv());
    }
}
