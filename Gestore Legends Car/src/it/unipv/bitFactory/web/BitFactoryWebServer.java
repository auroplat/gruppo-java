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
import it.unipv.bitFactory.controller.LoginController;
import it.unipv.bitFactory.service.GestoreEventi;
import it.unipv.bitFactory.service.GestoreSessioniLogin;
import it.unipv.bitFactory.web.handler.EventiApiHttpHandler;
import it.unipv.bitFactory.web.handler.LoginHttpHandler;
import it.unipv.bitFactory.web.handler.MagazzinoApiHttpHandler;
import it.unipv.bitFactory.web.handler.MagazzinoHttpHandler;
import it.unipv.bitFactory.web.handler.PezziLiberiApiHttpHandler;
import it.unipv.bitFactory.web.handler.PezziMontatiApiHttpHandler;
import it.unipv.bitFactory.web.handler.MacchineApiHttpHandler;
import it.unipv.bitFactory.web.handler.PrenotazioniHttpHandler;
import it.unipv.bitFactory.web.handler.SessioniHttpHandler;
import it.unipv.bitFactory.web.view.HtmlRenderer;
import it.unipv.bitFactory.model.persona.Ruolo;
import it.unipv.bitFactory.web.handler.LogoutHttpHandler;
import it.unipv.bitFactory.web.handler.RuoloHttpHandler;

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
            LoginController loginController,
            GestoreEventi gestoreEventi,
            GestoreSessioniLogin gestoreSessioniLogin,
            HtmlRenderer renderer) throws IOException {

        if (porta < 1 || porta > 65535) {
            throw new IllegalArgumentException("La porta deve essere compresa tra 1 e 65535");
        }

        if (numeroThread <= 0) {
            throw new IllegalArgumentException("Il numero di thread deve essere maggiore di zero");
        }

        if (sessioniController == null
                || magazzinoController == null
                || prenotazioniController == null
                || loginController == null
                || gestoreEventi == null
                || gestoreSessioniLogin == null
                || renderer == null) {

            throw new IllegalArgumentException("Controller, servizi e renderer non possono essere null");
        }
        
        this.porta = porta;

        server = HttpServer.create(new InetSocketAddress(porta), 0);

        threadPool = Executors.newFixedThreadPool(numeroThread);

        
        server.createContext(
                "/",
                this::gestisciHome
        );

        //eventi e prenotazioni
        server.createContext(
                "/eventi.html",
                creaHandlerRisorsaStatica(
                        "/eventi.html",
                        "/web/eventi.html",
                        "text/html; charset=UTF-8"
                )
        );
        
        server.createContext(
                "/gestione-eventi.html",
                new RuoloHttpHandler(
                        creaHandlerRisorsaStatica(
                                "/gestione-eventi.html",
                                "/web/gestione-eventi.html",
                                "text/html; charset=UTF-8"
                        ),
                        gestoreSessioniLogin,
                        Ruolo.EVENTI
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
        
        server.createContext(
                "/eventi",
                new EventiApiHttpHandler(
                        gestoreEventi,
                        false
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
        
        server.createContext(
                "/login.css",
                creaHandlerRisorsaStatica(
                        "/login.css",
                        "/web/login.css",
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

        //api

        
        server.createContext(
                "/api/macchine",
                new RuoloHttpHandler(
                        new MacchineApiHttpHandler(
                                sessioniController
                        ),
                        gestoreSessioniLogin,
                        Ruolo.SESSIONI,
                        Ruolo.MAGAZZINO
                )
        );
        
        server.createContext(
                "/api/magazzino",
                new RuoloHttpHandler(
                        new MagazzinoApiHttpHandler(
                                magazzinoController
                        ),
                        gestoreSessioniLogin,
                        Ruolo.MAGAZZINO
                )
        );
        
        server.createContext(
                "/api/pezzi-liberi",
                new RuoloHttpHandler(
                        new PezziLiberiApiHttpHandler(
                                magazzinoController
                        ),
                        gestoreSessioniLogin,
                        Ruolo.MAGAZZINO
                )
        );
        server.createContext(
                "/api/pezzi-montati",
                new RuoloHttpHandler(
                        new PezziMontatiApiHttpHandler(
                                magazzinoController
                        ),
                        gestoreSessioniLogin,
                        Ruolo.MAGAZZINO
                )
        );
        server.createContext(
                "/creazione-eventi",
                new RuoloHttpHandler(
                        new EventiApiHttpHandler(
                                gestoreEventi,
                                true
                        ),
                        gestoreSessioniLogin,
                        Ruolo.EVENTI
                )
        );
        
        
        //login e logout
        server.createContext(
                "/login.html",
                creaHandlerRisorsaStatica(
                        "/login.html",
                        "/web/login.html",
                        "text/html; charset=UTF-8"
                )
        );

        server.createContext(
                "/login",
                new LoginHttpHandler(
                        loginController,
                        gestoreSessioniLogin
                )
        );
        
        server.createContext(
                "/logout",
                new LogoutHttpHandler(
                        gestoreSessioniLogin
                )
        );

        //sessioni
        server.createContext(
                "/sessioni",
                new RuoloHttpHandler(
                        new SessioniHttpHandler(
                                sessioniController,
                                renderer
                        ),
                        gestoreSessioniLogin,
                        Ruolo.SESSIONI
                )
        );

        //magazzino
        server.createContext(
                "/magazzino",
                new RuoloHttpHandler(
                        new MagazzinoHttpHandler(
                                magazzinoController,
                                renderer
                        ),
                        gestoreSessioniLogin,
                        Ruolo.MAGAZZINO
                )
        );

        //prenotazioni
        server.createContext(
                "/prenotazioni",
                new PrenotazioniHttpHandler(
                        prenotazioniController,
                        renderer
                )
        );

        server.setExecutor(threadPool);
    }

    //home
    private void gestisciHome(
            HttpExchange exchange) throws IOException {

        String percorso = exchange.getRequestURI().getPath();

        if (!"/".equals(percorso)) {

            inviaErrore(
                    exchange,
                    404,
                    "Pagina non trovata"
            );

            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {

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

        exchange.getResponseHeaders().set(
                "Location",
                "/login.html"
        );

        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private HttpHandler creaHandlerRisorsaStatica(String percorsoHttp, String percorsoRisorsa, String contentType) {

        return exchange -> {

            String percorsoRichiesto = exchange.getRequestURI().getPath();

            if (!percorsoHttp.equals(percorsoRichiesto)) {

                inviaErrore(
                        exchange,
                        404,
                        "Risorsa non trovata"
                );

                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {

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

    private void inviaRisorsa(HttpExchange exchange, String percorsoRisorsa, String contentType) throws IOException {

        try (InputStream input = BitFactoryWebServer.class.getResourceAsStream(percorsoRisorsa)) {

            if (input == null) {

                inviaErrore(
                        exchange,
                        404,
                        "Risorsa non trovata: "
                                + percorsoRisorsa
                );

                return;
            }

            byte[] contenuto = input.readAllBytes();

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

            try (OutputStream output = exchange.getResponseBody()) {

                output.write(contenuto);
            }
        }
    }


    private void inviaErrore(HttpExchange exchange, int codice, String messaggio) throws IOException {

        byte[] contenuto = messaggio.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                codice,
                contenuto.length
        );

        try (OutputStream output = exchange.getResponseBody()) {

            output.write(contenuto);
        }
    }

    //server
    public void avvia() {

        server.start();

        System.out.println("Server BitFactory avviato su http://localhost:"+ porta);
    }

    public void arresta() {

        server.stop(0);
        threadPool.shutdown();
        System.out.println("Server BitFactory arrestato");
    }

    public int getPorta() {return porta;}
}