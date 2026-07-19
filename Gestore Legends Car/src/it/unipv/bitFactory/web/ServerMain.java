package it.unipv.bitFactory.web;

import java.nio.file.Files;
import java.nio.file.Path;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteLegendsDAO;
import it.unipv.bitFactory.model.sessioni.Gara;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.Test;
import it.unipv.bitFactory.thread.UsuraPezziThread;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class ServerMain {

    /*
     * Impostare true soltanto per provare il riempimento della coda.
     * Le sessioni di prova vengono realmente salvate sul database.
     */
    private static final boolean TEST_RIEMPIMENTO_CODA = false;
    private static final int NUMERO_RICHIESTE_TEST = 6;
    private static final String ID_MACCHINA_TEST = "MAC002";

    private ServerMain() {
    }

    public static void main(String[] args) {
        UsuraPezziThread usuraPezziThread = null;

        try {
            Path databasePath = Path.of(
                    "data",
                    "database_bfactory.db"
            ).toAbsolutePath().normalize();

            System.out.println(
                    "Database utilizzato: " + databasePath
            );
            System.out.println(
                    "Database esistente: "
                            + Files.isRegularFile(databasePath)
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

            usuraPezziThread =
                    new UsuraPezziThread(sessioniController);

            /*
             * Per mostrare sicuramente il riempimento, le richieste
             * vengono accodate prima dell'avvio del dispatcher.
             */
            if (TEST_RIEMPIMENTO_CODA) {
                caricaRichiesteDiTest(usuraPezziThread);
            }

            usuraPezziThread.start();

            if (TEST_RIEMPIMENTO_CODA) {
                avviaMonitorCoda(usuraPezziThread);
            }

            BitFactoryWebServer server =
                    new BitFactoryWebServer(
                            8082,
                            8,
                            sessioniController,
                            magazzinoController,
                            prenotazioniController,
                            usuraPezziThread,
                            renderer
                    );

            UsuraPezziThread usuraFinale =
                    usuraPezziThread;

            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            () -> arrestaTutto(
                                    server,
                                    usuraFinale
                            ),
                            "bitfactory-shutdown"
                    )
            );

            server.avvia();

        } catch (Exception e) {
            if (usuraPezziThread != null) {
                usuraPezziThread.arrestaThread();
            }

            System.err.println(
                    "Errore durante l'avvio del server: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    private static void caricaRichiesteDiTest(
            UsuraPezziThread dispatcher) {

        System.out.printf(
                "%n=== TEST CODA: inserimento di %d richieste ===%n",
                NUMERO_RICHIESTE_TEST
        );

        for (int indice = 1;
             indice <= NUMERO_RICHIESTE_TEST;
             indice++) {

            dispatcher.inviaAggiornamento(
                    ID_MACCHINA_TEST,
                    creaSessioneDiTest(indice)
            );

            System.out.printf(
                    "[main-test] richiesta %d inserita; "
                            + "dimensione coda: %d%n",
                    indice,
                    dispatcher.getNumeroRichiesteInCoda()
            );
        }

        System.out.printf(
                "=== CODA CARICATA: %d richieste; "
                        + "ora avvio il dispatcher ===%n%n",
                dispatcher.getNumeroRichiesteInCoda()
        );
    }

    private static Sessione creaSessioneDiTest(int indice) {
        String luogo = "Pista automatica " + indice;
        double kmPercorsi = 10.0 + indice;
        int tempoPassato = 60 + indice;

        if (indice % 2 == 0) {
            return new Gara(
                    luogo,
                    kmPercorsi,
                    tempoPassato,
                    indice
            );
        }

        return new Test(
                luogo,
                kmPercorsi,
                tempoPassato,
                "Sessione automatica " + indice
        );
    }

    private static void avviaMonitorCoda(
            UsuraPezziThread dispatcher) {

        Thread monitor = new Thread(
                () -> {
                    try {
                        for (int i = 0; i < 40; i++) {
                            System.out.printf(
                                    "[MONITOR] coda=%d; "
                                            + "worker attivi=%d; "
                                            + "permessi creazione=%d; "
                                            + "thread in attesa semaforo=%d%n",
                                    dispatcher
                                            .getNumeroRichiesteInCoda(),
                                    dispatcher
                                            .getNumeroWorkerAttivi(),
                                    dispatcher
                                            .getNumeroPermessiCreazioneDisponibili(),
                                    dispatcher
                                            .getNumeroThreadInAttesaDelSemaforo()
                            );

                            Thread.sleep(1_000L);
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                "monitor-coda"
        );

        monitor.setDaemon(true);
        monitor.start();
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
            thread.join(5_000L);

            if (thread.isAlive()) {
                thread.interrupt();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
