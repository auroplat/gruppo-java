package it.unipv.bitFactory.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import it.unipv.bitFactory.controller.*;
import it.unipv.bitFactory.thread.UsuraPezziThread;
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
            UsuraPezziThread usuraPezziThread,
            HtmlRenderer renderer) throws IOException {

        if (porta < 1 || porta > 65535) {
            throw new IllegalArgumentException("La porta deve essere compresa tra 1 e 65535");
        }

        if (numeroThread <= 0) {
            throw new IllegalArgumentException("Il numero di thread deve essere maggiore di zero");
        }

        Objects.requireNonNull(sessioniController);
        Objects.requireNonNull(magazzinoController);
        Objects.requireNonNull(prenotazioniController);
        Objects.requireNonNull(usuraPezziThread);
        Objects.requireNonNull(renderer);

        this.porta = porta;

        server = HttpServer.create(new InetSocketAddress(porta), 0);
        
        ///////////////////////////////////////////////////////////////////QUA///////////////////////////////////////////
        threadPool = Executors.newFixedThreadPool(numeroThread);
        server.setExecutor(threadPool);
        
        

        server.createContext("/", this::gestisciHome);

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
                "/styles.css",
                creaHandlerRisorsaStatica(
                        "/styles.css",
                        "/web/styles.css",
                        "text/css; charset=UTF-8"
                )
        );

        server.createContext(
                "/racing-bg.svg",
                creaHandlerRisorsaStatica(
                        "/racing-bg.svg",
                        "/web/racing-bg.svg",
                        "image/svg+xml"
                )
        );

        server.createContext(
                "/api/macchine",
                new MacchineApiHttpHandler(sessioniController)
        );

        server.createContext(
                "/sessioni",
                new SessioniHttpHandler(
                        usuraPezziThread,
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

    }

    private void gestisciHome(HttpExchange exchange)
            throws IOException {

        String percorso = exchange.getRequestURI().getPath();

        if (!"/".equals(percorso)) {
            inviaErrore(exchange, 404, "Pagina non trovata");
            return;
        }

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            inviaErrore(exchange, 405, "Metodo HTTP non consentito");
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
            String percorsoRichiesto = exchange.getRequestURI().getPath();

            if (!percorsoHttp.equals(percorsoRichiesto)) {
                inviaErrore(exchange, 404, "Risorsa non trovata");
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                inviaErrore(exchange, 405, "Metodo HTTP non consentito");
                return;
            }

            inviaRisorsa(exchange, percorsoRisorsa, contentType);
        };
    }

    private void inviaRisorsa(
            HttpExchange exchange,
            String percorsoRisorsa,
            String contentType) throws IOException {

        try (InputStream input = BitFactoryWebServer.class
                .getResourceAsStream(percorsoRisorsa)) {

            if (input == null) {
                inviaErrore(
                        exchange,
                        404,
                        "Risorsa non trovata: " + percorsoRisorsa
                );
                return;
            }

            byte[] contenuto = input.readAllBytes();

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, contenuto.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(contenuto);
            }
        }
    }

    private void inviaErrore(
            HttpExchange exchange,
            int codice,
            String messaggio) throws IOException {

        byte[] contenuto = messaggio.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(codice, contenuto.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(contenuto);
        }
    }

    public void avvia() {
        server.start();
        System.out.println(
                "Server BitFactory avviato su http://localhost:" + porta
        );
    }

    public void arresta() {
        server.stop(0);
        threadPool.shutdown();

        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Server BitFactory arrestato");
    }

    public int getPorta() {
        return porta;
    }
}
