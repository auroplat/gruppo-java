package it.unipv.bitFactory.web.handler.login;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import it.unipv.bitFactory.controller.GestioneLoginController;
import it.unipv.bitFactory.model.persona.Addetto;
import it.unipv.bitFactory.model.persona.Ruolo;
import it.unipv.bitFactory.service.ServizioSessioniLogin;

public class LoginHttpHandler implements HttpHandler {

    private final GestioneLoginController loginController;
    private final ServizioSessioniLogin gestoreSessioniLogin;

    public LoginHttpHandler(
            GestioneLoginController loginController,
            ServizioSessioniLogin gestoreSessioniLogin) {

        if (loginController == null) {
            throw new IllegalArgumentException(
                    "Il LoginController non può essere nullo"
            );
        }

        if (gestoreSessioniLogin == null) {
            throw new IllegalArgumentException(
                    "Il GestoreSessioniLogin non può essere nullo"
            );
        }

        this.loginController = loginController;
        this.gestoreSessioniLogin = gestoreSessioniLogin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            inviaMetodoNonConsentito(exchange);
            return;
        }

        gestisciLogin(exchange);
    }

    private void gestisciLogin(HttpExchange exchange)
            throws IOException {

        String corpoRichiesta = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Map<String, String> parametri =
                leggiParametriForm(corpoRichiesta);

        String username = parametri.get("username");
        String password = parametri.get("password");

        final Addetto addetto;

        try {
            addetto = loginController.login(
                    username,
                    password
            );
        } catch (RuntimeException e) {
            reindirizza(
                    exchange,
                    "/login.html?errore=server"
            );
            return;
        }

        if (addetto == null) {
            reindirizza(
                    exchange,
                    "/login.html?errore=credenziali"
            );
            return;
        }

        String sessionId =
                gestoreSessioniLogin.creaSessione(addetto);

        aggiungiCookieSessione(exchange, sessionId);
        reindirizzaInBaseAlRuolo(exchange, addetto.getRuolo());
    }

    private void aggiungiCookieSessione(
            HttpExchange exchange,
            String sessionId) {

        String cookie =
                ServizioSessioniLogin.NOME_COOKIE
                        + "=" + sessionId
                        + "; Path=/"
                        + "; HttpOnly"
                        + "; SameSite=Lax";

        exchange.getResponseHeaders().add(
                "Set-Cookie",
                cookie
        );
    }

    private void reindirizzaInBaseAlRuolo(
            HttpExchange exchange,
            Ruolo ruolo) throws IOException {

        if (ruolo == null) {
            reindirizza(
                    exchange,
                    "/login.html?errore=ruolo"
            );
            return;
        }

        switch (ruolo) {

            case MAGAZZINO ->
                    reindirizza(
                            exchange,
                            "/magazzino"
                    );

            case EVENTI ->
                    reindirizza(
                            exchange,
                            "/gestione-eventi.html"
                    );

            case SESSIONI ->
                    reindirizza(
                            exchange,
                            "/sessioni"
                    );

            default ->
                    reindirizza(
                            exchange,
                            "/login.html?errore=ruolo"
                    );
        }
    }

    private Map<String, String> leggiParametriForm(
            String corpoRichiesta) {

        Map<String, String> parametri = new HashMap<>();

        if (corpoRichiesta == null
                || corpoRichiesta.isBlank()) {
            return parametri;
        }

        String[] coppie = corpoRichiesta.split("&");

        for (String coppia : coppie) {

            String[] parti = coppia.split("=", 2);

            String chiave = decodifica(parti[0]);

            String valore = parti.length > 1
                    ? decodifica(parti[1])
                    : "";

            parametri.put(chiave, valore);
        }

        return parametri;
    }

    private String decodifica(String valore) {
        return URLDecoder.decode(
                valore,
                StandardCharsets.UTF_8
        );
    }

    private void reindirizza(
            HttpExchange exchange,
            String percorso) throws IOException {

        exchange.getResponseHeaders().set(
                "Location",
                percorso
        );

        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private void inviaMetodoNonConsentito(
            HttpExchange exchange) throws IOException {

        byte[] risposta =
                "Metodo HTTP non consentito"
                        .getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/plain; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
                "Allow",
                "POST"
        );

        exchange.sendResponseHeaders(
                405,
                risposta.length
        );

        exchange.getResponseBody().write(risposta);
        exchange.close();
    }
}

