package it.unipv.bitFactory.web;

import java.nio.file.Files;
import java.nio.file.Path;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteLegendsDAO;
import it.unipv.bitFactory.thread.DatabaseWriteLock;
import it.unipv.bitFactory.thread.MagazzinoThread;
import it.unipv.bitFactory.thread.UsuraPezziThread;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) {
        UsuraPezziThread usuraPezziThread = null;
        MagazzinoThread magazzinoThread = null;

        try {
            Path databasePath = Path.of(
                    "data",
                    "database_bfactory.db"
            ).toAbsolutePath().normalize();

            System.out.println("Database utilizzato: " + databasePath);
            System.out.println(
                    "Database esistente: " + Files.isRegularFile(databasePath)
            );

            if (!Files.isRegularFile(databasePath)) {
                throw new IllegalStateException(
                        "Database non trovato: " + databasePath
                );
            }

            LegendsDAO dao = new SqliteLegendsDAO(
                    databasePath.toString()
            );

            GestioneSessioniController sessioniController =
                    new GestioneSessioniController(dao);

            GestioneMagazzinoController magazzinoController =
                    new GestioneMagazzinoController();

            GestionePrenotazioniController prenotazioniController =
                    new GestionePrenotazioniController();

            HtmlRenderer renderer = new HtmlRenderer();

            // Una sola istanza condivisa: entrambi i thread usano lo stesso lock.
            DatabaseWriteLock databaseWriteLock =
                    new DatabaseWriteLock();

            usuraPezziThread = new UsuraPezziThread(
                    sessioniController,
                    databaseWriteLock
            );

            magazzinoThread = new MagazzinoThread(
                    databaseWriteLock
            );

            // start(), non run(): crea due thread reali distinti.
            usuraPezziThread.start();
            magazzinoThread.start();

            BitFactoryWebServer server = new BitFactoryWebServer(
                    8082,
                    8,
                    sessioniController,
                    magazzinoController,
                    prenotazioniController,
                    usuraPezziThread,
                    magazzinoThread,
                    renderer
            );

            UsuraPezziThread usuraFinale = usuraPezziThread;
            MagazzinoThread magazzinoFinale = magazzinoThread;

            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            () -> arrestaTutto(
                                    server,
                                    usuraFinale,
                                    magazzinoFinale
                            ),
                            "bitfactory-shutdown"
                    )
            );

            server.avvia();

        } catch (Exception e) {
            if (usuraPezziThread != null) {
                usuraPezziThread.arrestaThread();
            }

            if (magazzinoThread != null) {
                magazzinoThread.arrestaThread();
            }

            System.err.println(
                    "Errore durante l'avvio del server: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private static void arrestaTutto(
            BitFactoryWebServer server,
            UsuraPezziThread usuraPezziThread,
            MagazzinoThread magazzinoThread) {

        server.arresta();

        usuraPezziThread.arrestaThread();
        magazzinoThread.arrestaThread();

        attendiTerminazione(usuraPezziThread);
        attendiTerminazione(magazzinoThread);
    }

    private static void attendiTerminazione(Thread thread) {
        try {
            thread.join(5000);

            if (thread.isAlive()) {
                thread.interrupt();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
