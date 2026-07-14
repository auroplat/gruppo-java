package it.unipv.bitFactory.web;

import java.io.IOException;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.model.persona.Addetto;
import it.unipv.bitFactory.model.persona.Ruolo;
import it.unipv.bitFactory.service.GestoreSessioniLogin;

public final class ControlloAccesso {

    private ControlloAccesso() {
    }

    public static Addetto trovaAddettoAutenticato(
            HttpExchange exchange,
            GestoreSessioniLogin gestoreSessioniLogin) {

        if (exchange == null || gestoreSessioniLogin == null) {return null;}

        String sessionId = leggiSessionId(exchange);
        return gestoreSessioniLogin.trovaAddetto(sessionId);
    }

    public static boolean consentiRuolo(HttpExchange exchange,
    					GestoreSessioniLogin gestoreSessioniLogin, Ruolo ruoloRichiesto) throws IOException {

        if (exchange == null || gestoreSessioniLogin == null || ruoloRichiesto == null) {

            if (exchange != null) {
                reindirizzaAlLogin(exchange);
            }

            return false;
        }

        String sessionId = leggiSessionId(exchange);

        if (!gestoreSessioniLogin.haRuolo(
                sessionId,
                ruoloRichiesto
        )) {
            reindirizzaAlLogin(exchange);
            return false;
        }

        return true;
    }

    public static String leggiSessionId(HttpExchange exchange) {

        if (exchange == null) {return null;}

        List<String> cookieHeaders = exchange.getRequestHeaders().get("Cookie");

        if (cookieHeaders == null) {return null;}

        for (String cookieHeader : cookieHeaders) {

            String[] cookies = cookieHeader.split(";");

            for (String cookie : cookies) {

                String[] parti = cookie.trim().split("=", 2);

                if (parti.length == 2 && GestoreSessioniLogin.NOME_COOKIE.equals(parti[0])) {
                    return parti[1];
                }
            }
        }

        return null;
    }

    private static void reindirizzaAlLogin(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set(
                "Location",
                "/login.html?errore=accesso"
        );

        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }
}
