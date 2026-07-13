package it.unipv.bitFactory.web;

import java.nio.file.Files;
import java.nio.file.Path;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteLegendsDAO;
import it.unipv.bitFactory.thread.UsuraPezziThread;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) {
        UsuraPezziThread usuraPezziThread = null;

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

            // Il lock delle sessioni è completamente interno a
            // UsuraPezziThread: non serve alcun lock esterno condiviso.
            usuraPezziThread = new UsuraPezziThread(sessioniController);

            // start(), non run(): crea un thread reale distinto.
            usuraPezziThread.start();

            BitFactoryWebServer server = new BitFactoryWebServer(
                    8082,
                    8,
                    sessioniController,
                    magazzinoController,
                    prenotazioniController,
                    usuraPezziThread,
                    renderer
            );

            UsuraPezziThread usuraFinale = usuraPezziThread;

            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            () -> arrestaTutto(server, usuraFinale),
                            "bitfactory-shutdown"
                    )
            );

            server.avvia();

        } catch (Exception e) {
            if (usuraPezziThread != null) {
                usuraPezziThread.arrestaThread();
            }

            System.err.println(
                    "Errore durante l'avvio del server: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private static void arrestaTutto(
            BitFactoryWebServer server,
            UsuraPezziThread usuraPezziThread) {

        server.arresta();

        usuraPezziThread.arrestaThread();
        attendiTerminazione(usuraPezziThread);
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
