package de.foerderverein.kasse.app;

import de.foerderverein.kasse.config.AppConfig;
import de.foerderverein.kasse.hbci.CredentialStore;
import de.foerderverein.kasse.hbci.HbciImportService;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;
import de.foerderverein.kasse.service.BerichtService;
import de.foerderverein.kasse.service.BuchungService;
import de.foerderverein.kasse.service.GeschaeftsjahrService;
import de.foerderverein.kasse.service.KategorieService;
import de.foerderverein.kasse.service.KontoService;
import de.foerderverein.kasse.service.MitgliedService;

/** Bewusst kein DI-Framework (Guice/Spring) - bei dieser Groesse reicht eine simple, manuell
 * verdrahtete Composition Root, die beim Start einmal aufgebaut wird. */
public final class AppContext {

    public final KontoService kontoService;
    public final KategorieService kategorieService;
    public final BuchungService buchungService;
    public final MitgliedService mitgliedService;
    public final GeschaeftsjahrService geschaeftsjahrService;
    public final BerichtService berichtService;
    public final HbciImportService hbciImportService;
    public final CredentialStore credentialStore;

    private AppContext(KontoService kontoService, KategorieService kategorieService, BuchungService buchungService,
                        MitgliedService mitgliedService, GeschaeftsjahrService geschaeftsjahrService,
                        BerichtService berichtService, HbciImportService hbciImportService,
                        CredentialStore credentialStore) {
        this.kontoService = kontoService;
        this.kategorieService = kategorieService;
        this.buchungService = buchungService;
        this.mitgliedService = mitgliedService;
        this.geschaeftsjahrService = geschaeftsjahrService;
        this.berichtService = berichtService;
        this.hbciImportService = hbciImportService;
        this.credentialStore = credentialStore;
    }

    public static AppContext create() {
        Database database = new Database(AppConfig.databaseFile());
        database.migrate();

        KontoRepository kontoRepository = new KontoRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        BuchungRepository buchungRepository = new BuchungRepository(database);
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        GeschaeftsjahrRepository geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        CredentialStore credentialStore = new CredentialStore();

        return new AppContext(
                new KontoService(kontoRepository),
                new KategorieService(kategorieRepository),
                new BuchungService(buchungRepository, geschaeftsjahrRepository),
                new MitgliedService(mitgliedRepository),
                new GeschaeftsjahrService(geschaeftsjahrRepository),
                new BerichtService(buchungRepository, kategorieRepository, mitgliedRepository),
                new HbciImportService(buchungRepository, credentialStore),
                credentialStore);
    }
}
