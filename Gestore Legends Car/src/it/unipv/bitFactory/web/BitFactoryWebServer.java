package it.unipv.bitFactory.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.web.handler.MagazzinoHttpHandler;
import it.unipv.bitFactory.web.handler.PrenotazioniHttpHandler;
import it.unipv.bitFactory.web.handler.SessioniHttpHandler;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class BitFactoryWebServer {

    private final HttpServer server;
    private final ExecutorService threadPool;
    private final int porta;

    public BitFactoryWebServer(
            int porta,
            int numeroThread,
            GestioneSessioniController sessioniController,
            GestioneMagazzinoController magazzinoController,
            GestionePrenotazioniController prenotazioniController,
            HtmlRenderer renderer) throws IOException {

        if (porta < 1 || porta > 65535) {
            throw new IllegalArgumentException(
                    "La porta deve essere compresa tra 1 e 65535"
            );
        }

        if (numeroThread <= 0) {
            throw new IllegalArgumentException(
                    "Il numero di thread deve essere maggiore di zero"
            );
        }

        if (sessioniController == null
                || magazzinoController == null
                || prenotazioniController == null
                || renderer == null) {

            throw new IllegalArgumentException(
                    "Controller e renderer non possono essere null"
            );
        }

        this.porta = porta;

        server = HttpServer.create(
                new InetSocketAddress(porta),
                0
        );

        threadPool = Executors.newFixedThreadPool(numeroThread);

        /*
         * HOME
         *
         * GET /
         * restituisce il file:
         *
         * src/web/eventi.html
         */
        server.createContext(
                "/",
                this::gestisciHome
        );

        /*
         * Consente anche di aprire direttamente:
         *
         * http://localhost:8080/eventi.html
         */
        server.createContext(
                "/eventi.html",
                creaHandlerRisorsaStatica(
                        "/eventi.html",
                        "/web/eventi.html",
                        "text/html; charset=UTF-8"
                )
        );

        /*
         * Pagina contenente il form di prenotazione.
         *
         * GET /prenotazione?evento=...&data=...&luogo=...
         */
        server.createContext(
                "/prenotazione",
                creaHandlerRisorsaStatica(
                        "/prenotazione",
                        "/web/prenotazione.html",
                        "text/html; charset=UTF-8"
                )
        );

        /*
         * File CSS usato da entrambe le pagine HTML.
         */
        server.createContext(
                "/styles.css",
                creaHandlerRisorsaStatica(
                        "/styles.css",
                        "/web/styles.css",
                        "text/css; charset=UTF-8"
                )
        );

        /*
         * Immagine di sfondo.
         */
        server.createContext(
                "/racing-bg.svg",
                creaHandlerRisorsaStatica(
                        "/racing-bg.svg",
                        "/web/racing-bg.svg",
                        "image/svg+xml"
                )
        );

        /*
         * Rotte collegate ai controller applicativi.
         */
        server.createContext(
                "/sessioni",
                new SessioniHttpHandler(
                        sessioniController,
                        renderer
                )
        );

        server.createContext(
                "/magazzino",
                new MagazzinoHttpHandler(
                        magazzinoController,
                        renderer
                )
        );

        /*
         * Il form presente in prenotazione.html esegue:
         *
         * POST /prenotazioni
         *
         * Questa richiesta viene ricevuta da
         * PrenotazioniHttpHandler.
         */
        server.createContext(
                "/prenotazioni",
                new PrenotazioniHttpHandler(
                        prenotazioniController,
                        renderer
                )
        );

        server.setExecutor(threadPool);
    }

    /**
     * Gestisce esclusivamente la Home del server.
     */
    private void gestisciHome(HttpExchange exchange)
            throws IOException {

        String percorso = exchange.getRequestURI().getPath();

        /*
         * Il context "/" intercetta anche gli indirizzi non
         * associati ad altri context.
         *
         * Per questo controlliamo che il percorso sia
         * esattamente "/".
         */
        if (!"/".equals(percorso)) {
            inviaErrore(
                    exchange,
                    404,
                    "Pagina non trovata"
            );
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");

            inviaErrore(
                    exchange,
                    405,
                    "Metodo HTTP non consentito"
            );
            return;
        }

        inviaRisorsa(
                exchange,
                "/web/eventi.html",
                "text/html; charset=UTF-8"
        );
    }

    /**
     * Crea un handler per una risorsa statica.
     */
    private HttpHandler creaHandlerRisorsaStatica(
            String percorsoHttp,
            String percorsoRisorsa,
            String contentType) {

        return exchange -> {

            String percorsoRichiesto =
                    exchange.getRequestURI().getPath();

            if (!percorsoHttp.equals(percorsoRichiesto)) {
                inviaErrore(
                        exchange,
                        404,
                        "Risorsa non trovata"
                );
                return;
            }

            if (!"GET".equalsIgnoreCase(
                    exchange.getRequestMethod())) {

                exchange.getResponseHeaders().set(
                        "Allow",
                        "GET"
                );

                inviaErrore(
                        exchange,
                        405,
                        "Metodo HTTP non consentito"
                );
                return;
            }

            inviaRisorsa(
                    exchange,
                    percorsoRisorsa,
                    contentType
            );
        };
    }

    /**
     * Legge un file presente nelle risorse del progetto
     * e lo invia al browser.
     */
    private void inviaRisorsa(
            HttpExchange exchange,
            String percorsoRisorsa,
            String contentType) throws IOException {

        try (InputStream input =
                     BitFactoryWebServer.class.getResourceAsStream(
                             percorsoRisorsa
                     )) {

            if (input == null) {
                inviaErrore(
                        exchange,
                        404,
                        "Risorsa non trovata: " + percorsoRisorsa
                );
                return;
            }

            byte[] contenuto = input.readAllBytes();

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    contentType
            );

            /*
             * Durante lo sviluppo evita che il browser mostri
             * una vecchia versione dei file HTML o CSS.
             */
            exchange.getResponseHeaders().set(
                    "Cache-Control",
                    "no-cache"
            );

            exchange.sendResponseHeaders(
                    200,
                    contenuto.length
            );

            try (OutputStream output =
                         exchange.getResponseBody()) {

                output.write(contenuto);
            }
        }
    }

    /**
     * Invia una risposta testuale di errore.
     */
    private void inviaErrore(
            HttpExchange exchange,
            int codice,
            String messaggio) throws IOException {

        byte[] contenuto =
                messaggio.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                codice,
                contenuto.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(contenuto);
        }
    }

    public void avvia() {
        server.start();

        System.out.println(
                "Server BitFactory avviato su http://localhost:"
                        + porta
        );
    }

    public void arresta() {
        server.stop(0);
        threadPool.shutdown();

        System.out.println("Server BitFactory arrestato");
    }

    public int getPorta() {
        return porta;
    }
}