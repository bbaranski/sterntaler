package de.foerderverein.kasse.app;

import java.nio.file.Path;

import de.foerderverein.kasse.hbci.CredentialStore;
import de.foerderverein.kasse.hbci.HbciImportService;
import de.foerderverein.kasse.persistence.AktivitaetRepository;
import de.foerderverein.kasse.persistence.AuditLogRepository;
import de.foerderverein.kasse.persistence.BelegRepository;
import de.foerderverein.kasse.persistence.BuchungRepository;
import de.foerderverein.kasse.persistence.Database;
import de.foerderverein.kasse.persistence.GeschaeftsjahrRepository;
import de.foerderverein.kasse.persistence.KategorieRepository;
import de.foerderverein.kasse.persistence.KontoRepository;
import de.foerderverein.kasse.persistence.MitgliedRepository;
import de.foerderverein.kasse.persistence.VereinRepository;
import de.foerderverein.kasse.service.AktivitaetService;
import de.foerderverein.kasse.service.BackupService;
import de.foerderverein.kasse.service.BelegService;
import de.foerderverein.kasse.service.BerichtService;
import de.foerderverein.kasse.service.BuchungService;
import de.foerderverein.kasse.service.GeschaeftsjahrService;
import de.foerderverein.kasse.service.KategorieService;
import de.foerderverein.kasse.service.KontoService;
import de.foerderverein.kasse.service.MitgliedService;
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
    public final BackupService backupService;
    public final HbciImportService hbciImportService;
    public final CredentialStore credentialStore;
    public final VereinService vereinService;

    private AppContext(KontoService kontoService, KategorieService kategorieService, BuchungService buchungService,
                        MitgliedService mitgliedService, AktivitaetService aktivitaetService, BelegService belegService,
                        GeschaeftsjahrService geschaeftsjahrService, BerichtService berichtService,
                        BackupService backupService, HbciImportService hbciImportService,
                        CredentialStore credentialStore, VereinService vereinService) {
        this.kontoService = kontoService;
        this.kategorieService = kategorieService;
        this.buchungService = buchungService;
        this.mitgliedService = mitgliedService;
        this.aktivitaetService = aktivitaetService;
        this.belegService = belegService;
        this.geschaeftsjahrService = geschaeftsjahrService;
        this.berichtService = berichtService;
        this.backupService = backupService;
        this.hbciImportService = hbciImportService;
        this.credentialStore = credentialStore;
        this.vereinService = vereinService;
    }

    public static AppContext create(Path databaseFile) {
        Database database = new Database(databaseFile);
        database.migrate();

        KontoRepository kontoRepository = new KontoRepository(database);
        KategorieRepository kategorieRepository = new KategorieRepository(database);
        BuchungRepository buchungRepository = new BuchungRepository(database);
        MitgliedRepository mitgliedRepository = new MitgliedRepository(database);
        AktivitaetRepository aktivitaetRepository = new AktivitaetRepository(database);
        BelegRepository belegRepository = new BelegRepository(database);
        GeschaeftsjahrRepository geschaeftsjahrRepository = new GeschaeftsjahrRepository(database);
        AuditLogRepository auditLogRepository = new AuditLogRepository(database);
        VereinRepository vereinRepository = new VereinRepository(database);
        CredentialStore credentialStore = new CredentialStore();
        MitgliedService mitgliedService = new MitgliedService(mitgliedRepository, auditLogRepository);

        return new AppContext(
                new KontoService(kontoRepository),
                new KategorieService(kategorieRepository),
                new BuchungService(buchungRepository, geschaeftsjahrRepository, auditLogRepository,
                        kategorieRepository, aktivitaetRepository),
                mitgliedService,
                new AktivitaetService(aktivitaetRepository),
                new BelegService(belegRepository),
                new GeschaeftsjahrService(geschaeftsjahrRepository),
                new BerichtService(buchungRepository, kategorieRepository, mitgliedService, vereinRepository),
                new BackupService(database, buchungRepository, kontoRepository, kategorieRepository),
                new HbciImportService(buchungRepository, credentialStore),
                credentialStore,
                new VereinService(vereinRepository));
    }
}
