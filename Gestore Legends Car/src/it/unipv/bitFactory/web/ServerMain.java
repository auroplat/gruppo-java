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

    private static final boolean TEST_RIEMPIMENTO_CODA = true;

    private static final int NUMERO_RICHIESTE_TEST = 6;

    private static final String ID_MACCHINA_TEST = "MAC002";

    private static final int PORTA_SERVER = 8082;

    private static final int NUMERO_THREAD_HTTP = 8;

    private ServerMain() {}

    public static void main(String[] args) {

        Dispatcher dispatcherSessioni = null;
        BitFactoryWebServer server = null;

        try {

            Path databasePath = Path.of("data","database_bfactory.db").toAbsolutePath().normalize();
            System.out.println( "Database utilizzato: "+ databasePath);
            System.out.println("Database esistente: "+ Files.isRegularFile(databasePath));
            if (!Files.isRegularFile(databasePath)) {throw new IllegalStateException("Database non trovato: " + databasePath);}
            LegendsDAO dao = new SqliteLegendsDAO(databasePath.toString());
            GestioneSessioniController sessioniController = new GestioneSessioniController(dao);
            GestioneMagazzinoController magazzinoController = new GestioneMagazzinoController();
            GestionePrenotazioniController prenotazioniController = new GestionePrenotazioniController();
            HtmlRenderer renderer =new HtmlRenderer();

            
            
            //creo dispatcher
            dispatcherSessioni = new Dispatcher(sessioniController);

            //test per riempire la code
            if (TEST_RIEMPIMENTO_CODA) {
                caricaRichiesteDiTest(
                        dispatcherSessioni
                );
            }

            //avvio dispatcher
            dispatcherSessioni.start();

            //monitor
            if (TEST_RIEMPIMENTO_CODA) {
                avviaMonitor(dispatcherSessioni);
            }

            

            server = new BitFactoryWebServer(PORTA_SERVER,NUMERO_THREAD_HTTP,sessioniController, magazzinoController, prenotazioniController,dispatcherSessioni,renderer);
            BitFactoryWebServer serverFinale = server;

            Dispatcher dispatcherFinale = dispatcherSessioni;

            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            () -> arrestaApplicazione(
                                    serverFinale,
                                    dispatcherFinale
                            ),
                            "bitfactory-shutdown"
                    )
            );

            server.avvia();

        } catch (Exception e) {

            System.err.println( "Errore durante l'avvio dell'applicazione: "+ e.getMessage() );

            e.printStackTrace();
            
            
            
            


            if (dispatcherSessioni != null) {dispatcherSessioni.arrestaThread();}

            if (server != null) {server.arresta();}
        }
    }

    
    
    
    //inserisco le richiste prima di iniziare
    private static void caricaRichiesteDiTest(Dispatcher dispatcher) {

        System.out.printf("%n=== TEST CODA: inserimento di %d richieste ===%n", NUMERO_RICHIESTE_TEST);

        for (int indice = 1; indice <= NUMERO_RICHIESTE_TEST; indice++) {

            Sessione sessione = creaSessioneDiTest(indice);

            dispatcher.inviaAggiornamento(ID_MACCHINA_TEST,sessione);

            System.out.printf("[main-test] richiesta %d inserita; dimensione coda: %d%n", indice, dispatcher.getNumeroRichiesteInCoda());
        }

        System.out.printf("=== CODA CARICATA: %d richieste; ora avvio il dispatcher ===%n%n", dispatcher.getNumeroRichiesteInCoda());
    }

    //crea sessioni
    private static Sessione creaSessioneDiTest(int indice) {
        String luogo = "Pista automatica ";
        double kmPercorsi = 10.0;
        int tempoPassato = 60;
        return new Gara(luogo,kmPercorsi,tempoPassato,indice);
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
