package it.unipv.bitFactory.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.*;

import it.unipv.bitFactory.controller.*;
import it.unipv.bitFactory.service.GestoreEventi;
import it.unipv.bitFactory.web.handler.*;
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
            GestoreEventi gestoreEventi,
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

        if (sessioniController == null || magazzinoController == null || prenotazioniController == null
            || gestoreEventi == null || renderer == null) {

            throw new IllegalArgumentException("Controller, gestore eventi e renderer non possono essere null");
        }

        this.porta = porta;

        server = HttpServer.create(new InetSocketAddress(porta), 0);

        threadPool = Executors.newFixedThreadPool(numeroThread);

        server.createContext(
                "/",
                this::gestisciHome
        );

       
        server.createContext(
                "/eventi.html",
                creaHandlerRisorsaStatica(
                        "/eventi.html",
                        "/web/eventi.html",
                        "text/html; charset=UTF-8"
                )
        );

        server.createContext(
                "/prenotazione",
                creaHandlerRisorsaStatica(
                        "/prenotazione",
                        "/web/prenotazione.html",
                        "text/html; charset=UTF-8"
                )
        );

        server.createContext(
                "/prenotazione.html",
                creaHandlerRisorsaStatica(
                        "/prenotazione.html",
                        "/web/prenotazione.html",
                        "text/html; charset=UTF-8"
                )
        );

        //css
        server.createContext(
                "/styles.css",
                creaHandlerRisorsaStatica(
                        "/styles.css",
                        "/web/styles.css",
                        "text/css; charset=UTF-8"
                )
        );

        server.createContext(
                "/styles1.css",
                creaHandlerRisorsaStatica(
                        "/styles1.css",
                        "/web/styles1.css",
                        "text/css; charset=UTF-8"
                )
        );
        
        server.createContext(
                "/stylesM.css",
                creaHandlerRisorsaStatica(
                        "/stylesM.css",
                        "/web/stylesM.css",
                        "text/css; charset=UTF-8"
                )
        );

        //sfondo
        server.createContext(
                "/racing-bg.svg",
                creaHandlerRisorsaStatica(
                        "/racing-bg.svg",
                        "/web/racing-bg.svg",
                        "image/svg+xml"
                )
        );


        server.createContext(
                "/api/eventi",
                new EventiApiHttpHandler(
                        gestoreEventi
                )
        );

        server.createContext(
                "/api/macchine",
                new MacchineApiHttpHandler(
                        sessioniController
                )
        );

        server.createContext(
                "/api/magazzino",
                new MagazzinoApiHttpHandler(
                        magazzinoController
                )
        );

        server.createContext(
                "/api/pezzi-liberi",
                new PezziLiberiApiHttpHandler(
                        magazzinoController
                )
        );


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
        
        server.createContext(
                "/prenotazioni",
                new PrenotazioniHttpHandler(
                        prenotazioniController,
                        renderer
                )
        );

        server.setExecutor(threadPool);
    }

    private void gestisciHome(
            HttpExchange exchange) throws IOException {

        String percorso =
                exchange.getRequestURI().getPath();

        if (!"/".equals(percorso)) {

            inviaErrore(
                    exchange,
                    404,
                    "Pagina non trovata"
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
                "/web/eventi.html",
                "text/html; charset=UTF-8"
        );
    }

    private HttpHandler creaHandlerRisorsaStatica(
            String percorsoHttp,
            String percorsoRisorsa,
            String contentType) {

        return exchange -> {

            String percorsoRichiesto =
                    exchange.getRequestURI().getPath();

            if (!percorsoHttp.equals(
                    percorsoRichiesto)) {

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

    private void inviaRisorsa(
            HttpExchange exchange,
            String percorsoRisorsa,
            String contentType) throws IOException {

        try (InputStream input =
                     BitFactoryWebServer.class
                             .getResourceAsStream(
                                     percorsoRisorsa
                             )) {

            if (input == null) {

                inviaErrore(
                        exchange,
                        404,
                        "Risorsa non trovata: "
                                + percorsoRisorsa
                );

                return;
            }

            byte[] contenuto =
                    input.readAllBytes();

            exchange.getResponseHeaders().set(
                    "Content-Type",
                    contentType
            );

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

    private void inviaErrore(
            HttpExchange exchange,
            int codice,
            String messaggio) throws IOException {

        byte[] contenuto =
                messaggio.getBytes(
                        StandardCharsets.UTF_8
                );

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

    //avvio server
    public void avvia() {

        server.start();

        System.out.println(
                "Server BitFactory avviato su "
                        + "http://localhost:"
                        + porta
        );
    }

    
    //Arresta il server e il pool di thread.
    
    public void arresta() {

        server.stop(0);
        threadPool.shutdown();

        System.out.println(
                "Server BitFactory arrestato"
        );
    }

    public int getPorta() {
        return porta;
    }
}