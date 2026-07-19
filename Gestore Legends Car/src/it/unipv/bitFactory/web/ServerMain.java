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
import it.unipv.bitFactory.thread.Dispatcher;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class ServerMain {

    /*
     * true:
     * inserisce automaticamente sei richieste prima di avviare
     * il dispatcher, così puoi vedere il funzionamento di:
     *
     * - coda delle richieste;
     * - semaforo con tre permessi;
     * - tre worker contemporanei;
     * - lock fair per l'accesso al database.
     *
     * false:
     * avvio normale del server web.
     */
    private static final boolean TEST_RIEMPIMENTO_CODA = true;

    private static final int NUMERO_RICHIESTE_TEST = 6;

    private static final String ID_MACCHINA_TEST =
            "MAC002";

    private static final int PORTA_SERVER = 8082;

    private static final int NUMERO_THREAD_HTTP = 8;

    private ServerMain() {
    }

    public static void main(String[] args) {

        Dispatcher dispatcherSessioni = null;
        BitFactoryWebServer server = null;

        try {

            /*
             * Individuazione del database.
             */
            Path databasePath = Path.of(
                    "data",
                    "database_bfactory.db"
            ).toAbsolutePath().normalize();

            System.out.println(
                    "Database utilizzato: "
                            + databasePath
            );

            System.out.println(
                    "Database esistente: "
                            + Files.isRegularFile(databasePath)
            );

            if (!Files.isRegularFile(databasePath)) {
                throw new IllegalStateException(
                        "Database non trovato: "
                                + databasePath
                );
            }

            /*
             * DAO.
             */
            LegendsDAO dao =
                    new SqliteLegendsDAO(
                            databasePath.toString()
                    );

            /*
             * Controller.
             */
            GestioneSessioniController sessioniController =
                    new GestioneSessioniController(dao);

            GestioneMagazzinoController magazzinoController =
                    new GestioneMagazzinoController();

            GestionePrenotazioniController prenotazioniController =
                    new GestionePrenotazioniController();

            HtmlRenderer renderer =
                    new HtmlRenderer();

            /*
             * Creazione del dispatcher.
             *
             * Al suo interno sono presenti:
             *
             * - Semaphore(3) per limitare i worker;
             * - ReentrantLock(true) per il database;
             * - lock e Condition per la coda.
             */
            dispatcherSessioni =
                    new Dispatcher(
                            sessioniController
                    );

            /*
             * Nel test le richieste vengono accodate prima
             * dell'avvio del dispatcher.
             *
             * In questo modo la coda arriva sicuramente a 6.
             */
            if (TEST_RIEMPIMENTO_CODA) {
                caricaRichiesteDiTest(
                        dispatcherSessioni
                );
            }

            /*
             * Avvio del dispatcher.
             *
             * Ora potrà estrarre al massimo tre richieste,
             * perché il semaforo possiede tre permessi.
             */
            dispatcherSessioni.start();

            /*
             * Monitor didattico dello stato concorrente.
             */
            if (TEST_RIEMPIMENTO_CODA) {
                avviaMonitor(
                        dispatcherSessioni
                );
            }

            /*
             * Creazione del server HTTP.
             *
             * Lo stesso dispatcher viene passato agli handler,
             * così tutte le richieste HTTP usano la medesima coda.
             */
            server = new BitFactoryWebServer(
                    PORTA_SERVER,
                    NUMERO_THREAD_HTTP,
                    sessioniController,
                    magazzinoController,
                    prenotazioniController,
                    dispatcherSessioni,
                    renderer
            );

            /*
             * Copie finali necessarie per la lambda
             * dello shutdown hook.
             */
            BitFactoryWebServer serverFinale =
                    server;

            Dispatcher dispatcherFinale =
                    dispatcherSessioni;

            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            () -> arrestaApplicazione(
                                    serverFinale,
                                    dispatcherFinale
                            ),
                            "bitfactory-shutdown"
                    )
            );

            /*
             * Avvio del server HTTP.
             */
            server.avvia();

        } catch (Exception e) {

            System.err.println(
                    "Errore durante l'avvio "
                            + "dell'applicazione: "
                            + e.getMessage()
            );

            e.printStackTrace();

            /*
             * Se il dispatcher era già stato creato,
             * viene arrestato.
             */
            if (dispatcherSessioni != null) {
                dispatcherSessioni.arrestaThread();
            }

            /*
             * Se il server era già stato creato,
             * viene arrestato.
             */
            if (server != null) {
                server.arresta();
            }
        }
    }

    /**
     * Inserisce sei richieste nella coda prima
     * dell'avvio del dispatcher.
     */
    private static void caricaRichiesteDiTest(
            Dispatcher dispatcher) {

        System.out.printf(
                "%n=== TEST CODA: inserimento di %d richieste ===%n",
                NUMERO_RICHIESTE_TEST
        );

        for (int indice = 1;
             indice <= NUMERO_RICHIESTE_TEST;
             indice++) {

            Sessione sessione =
                    creaSessioneDiTest(indice);

            dispatcher.inviaAggiornamento(
                    ID_MACCHINA_TEST,
                    sessione
            );

            System.out.printf(
                    "[main-test] richiesta %d inserita; "
                            + "dimensione coda: %d%n",
                    indice,
                    dispatcher
                            .getNumeroRichiesteInCoda()
            );
        }

        System.out.printf(
                "=== CODA CARICATA: %d richieste; "
                        + "ora avvio il dispatcher ===%n%n",
                dispatcher.getNumeroRichiesteInCoda()
        );
    }

    /**
     * Crea alternativamente oggetti Test e Gara.
     */
    private static Sessione creaSessioneDiTest(
            int indice) {

        String luogo =
                "Pista automatica " + indice;

        double kmPercorsi =
                10.0 + indice;

        int tempoPassato =
                60 + indice;

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

    /**
     * Stampa periodicamente lo stato:
     *
     * - richieste ancora nella coda;
     * - worker attivi;
     * - permessi disponibili;
     * - dispatcher in attesa del semaforo;
     * - worker in attesa del lock database.
     */
    private static void avviaMonitor(
            Dispatcher dispatcher) {

        Thread monitor = new Thread(
                () -> {

                    String statoPrecedente = null;

                    try {
                        while (!Thread.currentThread().isInterrupted()) {

                            int richiesteInCoda =
                                    dispatcher.getNumeroRichiesteInCoda();

                            int workerAttivi =
                                    dispatcher.getNumeroWorkerAttivi();

                            int permessiDisponibili =
                                    dispatcher
                                            .getNumeroPermessiWorkerDisponibili();

                            int dispatcherInAttesa =
                                    dispatcher
                                            .getNumeroDispatcherInAttesaDelSemaforo();

                            int workerInAttesaLock =
                                    dispatcher
                                            .getNumeroWorkerInAttesaDelLockDatabase();

                            String statoAttuale = String.format(
                                    "[MONITOR] coda=%d; "
                                            + "worker attivi=%d; "
                                            + "permessi worker=%d; "
                                            + "dispatcher in attesa semaforo=%d; "
                                            + "worker in attesa lock DB=%d",
                                    richiesteInCoda,
                                    workerAttivi,
                                    permessiDisponibili,
                                    dispatcherInAttesa,
                                    workerInAttesaLock
                            );

                            /*
                             * Stampa soltanto se lo stato è diverso
                             * da quello rilevato precedentemente.
                             */
                            if (!statoAttuale.equals(statoPrecedente)) {
                                System.out.println(statoAttuale);
                                statoPrecedente = statoAttuale;
                            }

                            /*
                             * Termina il monitor quando tutto il lavoro
                             * è stato completato.
                             */
                            if (richiesteInCoda == 0
                                    && workerAttivi == 0
                                    && permessiDisponibili == 3) {

                                System.out.println(
                                        "[MONITOR] elaborazione completata"
                                );

                                break;
                            }

                            Thread.sleep(200L);
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                "monitor-concorrenza"
        );

        monitor.setDaemon(true);
        monitor.start();
    }

    /**
     * Arresta server e dispatcher.
     */
    private static void arrestaApplicazione(
            BitFactoryWebServer server,
            Dispatcher dispatcher) {

        System.out.println(
                "Arresto dell'applicazione..."
        );

        server.arresta();

        dispatcher.arrestaThread();

        attendiTerminazioneDispatcher(
                dispatcher
        );
    }

    /**
     * Attende per un massimo di cinque secondi
     * la terminazione del dispatcher.
     */
    private static void attendiTerminazioneDispatcher(
            Thread dispatcher) {

        try {

            dispatcher.join(5_000L);

            if (dispatcher.isAlive()) {

                System.out.println(
                        "Il dispatcher non è ancora terminato; "
                                + "invio interrupt."
                );

                dispatcher.interrupt();
            }

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();
        }
    }
}
