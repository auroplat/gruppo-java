package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import it.unipv.bitFactory.service.GestoreSessioniLogin;
import it.unipv.bitFactory.web.ControlloAccesso;

public final class LogoutHttpHandler implements HttpHandler {

    private final GestoreSessioniLogin gestoreSessioniLogin;

    public LogoutHttpHandler(
            GestoreSessioniLogin gestoreSessioniLogin) {

        if (gestoreSessioniLogin == null) {
            throw new IllegalArgumentException(
                    "Il GestoreSessioniLogin non può essere nullo"
            );
        }

        this.gestoreSessioniLogin = gestoreSessioniLogin;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"POST".equalsIgnoreCase(
                        exchange.getRequestMethod())) {

            inviaMetodoNonConsentito(exchange);
            return;
        }

        String sessionId =
                ControlloAccesso.leggiSessionId(exchange);

        gestoreSessioniLogin.eliminaSessione(sessionId);

        exchange.getResponseHeaders().add(
                "Set-Cookie",
                GestoreSessioniLogin.NOME_COOKIE
                        + "=; Path=/"
                        + "; Max-Age=0"
                        + "; HttpOnly"
                        + "; SameSite=Lax"
        );

        exchange.getResponseHeaders().set(
                "Location",
                "/login.html"
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
                "GET, POST"
        );

        exchange.sendResponseHeaders(
                405,
                risposta.length
        );

        exchange.getResponseBody().write(risposta);
        exchange.close();
    }
}
