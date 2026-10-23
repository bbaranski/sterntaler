package de.foerderverein.kasse.app;

import java.nio.file.Path;

import de.foerderverein.kasse.config.AppConfig;
import de.foerderverein.kasse.hbci.CredentialStore;
import de.foerderverein.kasse.hbci.HbciImportService;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.BelegRepository;
import de.foerderverein.kasse.persistence.BriefvorlageRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;
import de.foerderverein.kasse.persistence.VereinRepository;
import de.foerderverein.kasse.service.AktivitaetService;
import de.foerderverein.kasse.service.BelegService;
import de.foerderverein.kasse.service.BerichtService;
import de.foerderverein.kasse.service.BriefvorlageService;
import de.foerderverein.kasse.service.BuchungService;
import de.foerderverein.kasse.service.GeschaeftsjahrService;
import de.foerderverein.kasse.service.KategorieService;
import de.foerderverein.kasse.service.KontobewegungenExportService;
import de.foerderverein.kasse.service.KontoService;
import de.foerderverein.kasse.service.MitgliedService;
import de.foerderverein.kasse.service.SerienbriefService;
import de.foerderverein.kasse.service.SicherheitskopieService;
import de.foerderverein.kasse.service.VereinService;

/** Bewusst kein DI-Framework (Guice/Spring) - bei dieser Groesse reicht eine simple, manuell
 * verdrahtete Composition Root, die beim Start einmal aufgebaut wird. */
public final class AppContext {

    public final KontoService kontoService;
    public final KategorieService kategorieService;
    public final BuchungService buchungService;
    public final MitgliedService mitgliedService;
    public final AktivitaetService aktivitaetService;
    public final BelegService belegService;
    public final GeschaeftsjahrService geschaeftsjahrService;
    public final BerichtService berichtService;
    public final KontobewegungenExportService kontobewegungenExportService;
    public final SicherheitskopieService sicherheitskopieService;
    public final BriefvorlageService briefvorlageService;
    public final SerienbriefService serienbriefService;
    public final HbciImportService hbciImportService;
    public final CredentialStore credentialStore;
    public final VereinService vereinService;

    private AppContext(KontoService kontoService, KategorieService kategorieService, BuchungService buchungService,
                        MitgliedService mitgliedService, AktivitaetService aktivitaetService, BelegService belegService,
                        GeschaeftsjahrService geschaeftsjahrService, BerichtService berichtService,
                        KontobewegungenExportService kontobewegungenExportService,
                        SicherheitskopieService sicherheitskopieService, BriefvorlageService briefvorlageService,
                        SerienbriefService serienbriefService, HbciImportService hbciImportService,
                        CredentialStore credentialStore, VereinService vereinService) {
        this.kontoService = kontoService;
        this.kategorieService = kategorieService;
        this.buchungService = buchungService;
        this.mitgliedService = mitgliedService;
        this.aktivitaetService = aktivitaetService;
        this.belegService = belegService;
        this.geschaeftsjahrService = geschaeftsjahrService;
        this.berichtService = berichtService;
        this.kontobewegungenExportService = kontobewegungenExportService;
        this.sicherheitskopieService = sicherheitskopieService;
        this.briefvorlageService = briefvorlageService;
        this.serienbriefService = serienbriefService;
        this.hbciImportService = hbciImportService;
        this.credentialStore = credentialStore;
        this.vereinService = vereinService;
    }

    public static AppContext create(Path databaseFile) {
        Database database = new Database(databaseFile);
        database.migrate();

        Path belegeVerzeichnis = AppConfig.belegeVerzeichnis(databaseFile);
        Path vorlagenVerzeichnis = AppConfig.vorlagenVerzeichnis(databaseFile);

        KontoRepository kontoRepository = new KontoRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        BuchungRepository buchungRepository = new BuchungRepository(database);
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        AktivitaetRepository aktivitaetRepository = new AktivitaetRepository(database);
        BelegRepository belegRepository = new BelegRepository(database);
        BriefvorlageRepository briefvorlageRepository = new BriefvorlageRepository(database);
        GeschaeftsjahrRepository geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        AuditLogRepository auditLogRepository = new AuditLogRepository(database);
        VereinRepository vereinRepository = new VereinRepository(database);
        CredentialStore credentialStore = new CredentialStore();
        MitgliedService mitgliedService = new MitgliedService(mitgliedRepository, auditLogRepository);
        GeschaeftsjahrService geschaeftsjahrService = new GeschaeftsjahrService(geschaeftsjahrRepository);
        BerichtService berichtService = new BerichtService(buchungRepository, kategorieRepository,
                aktivitaetRepository, mitgliedService, geschaeftsjahrService, vereinRepository);
        KontobewegungenExportService kontobewegungenExportService = new KontobewegungenExportService(
                buchungRepository, kontoRepository, kategorieRepository, aktivitaetRepository,
                geschaeftsjahrService);
        BriefvorlageService briefvorlageService = new BriefvorlageService(briefvorlageRepository, vorlagenVerzeichnis);

        return new AppContext(
                new KontoService(kontoRepository),
                new KategorieService(kategorieRepository),
                new BuchungService(buchungRepository, geschaeftsjahrRepository, geschaeftsjahrService,
                        auditLogRepository, kategorieRepository, aktivitaetRepository),
                mitgliedService,
                new AktivitaetService(aktivitaetRepository),
                new BelegService(belegRepository, geschaeftsjahrService, belegeVerzeichnis),
                geschaeftsjahrService,
                berichtService,
                kontobewegungenExportService,
                new SicherheitskopieService(kontobewegungenExportService, berichtService, belegeVerzeichnis,
                        databaseFile),
                briefvorlageService,
                new SerienbriefService(vorlagenVerzeichnis, vereinRepository),
                new HbciImportService(buchungRepository, credentialStore),
                credentialStore,
                new VereinService(vereinRepository));
    }
}
