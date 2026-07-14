package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class BaseHttpHandler implements HttpHandler {

    protected boolean isGet(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }

    protected boolean isPost(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }

    protected Map<String, String> leggiParametriForm(
            HttpExchange exchange) throws IOException {

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");

        if (contentType == null
                || !contentType.toLowerCase().startsWith(
                        "application/x-www-form-urlencoded"
                )) {
            throw new IllegalArgumentException(
                    "Formato del form non supportato"
            );
        }

        String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
        );

        Map<String, String> parametri = new LinkedHashMap<>();

        if (body.isBlank()) {
            return parametri;
        }

        for (String coppia : body.split("&")) {
            String[] parti = coppia.split("=", 2);

            String nome = decodifica(parti[0]);
            String valore = parti.length == 2
                    ? decodifica(parti[1])
                    : "";

            parametri.put(nome, valore);
        }

        return parametri;
    }

    protected String parametroObbligatorio(
            Map<String, String> parametri,
            String nome) {

        String valore = parametri.get(nome);

        if (valore == null || valore.isBlank()) {
            throw new IllegalArgumentException(
                    "Parametro obbligatorio mancante: " + nome
            );
        }

        return valore.trim();
    }

    protected void sendHtml(
            HttpExchange exchange,
            int statusCode,
            String html) throws IOException {

        sendBytes(
                exchange,
                statusCode,
                html.getBytes(StandardCharsets.UTF_8),
                "text/html; charset=UTF-8"
        );
    }

    protected void sendJson(
            HttpExchange exchange,
            int statusCode,
            String json) throws IOException {

        sendBytes(
                exchange,
                statusCode,
                json.getBytes(StandardCharsets.UTF_8),
                "application/json; charset=UTF-8"
        );
    }

    protected void redirect(
            HttpExchange exchange,
            String indirizzo) throws IOException {

        exchange.getResponseHeaders().set("Location", indirizzo);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    protected void sendResource(
            HttpExchange exchange,
            String resourcePath,
            String contentType) throws IOException {

        try (var input = BaseHttpHandler.class
                .getResourceAsStream(resourcePath)) {

            if (input == null) {
                sendHtml(exchange, 404, "<h1>Risorsa non trovata</h1>");
                return;
            }

            sendBytes(
                    exchange,
                    200,
                    input.readAllBytes(),
                    contentType
            );
        }
    }

    private String decodifica(String valore) {
        return URLDecoder.decode(valore, StandardCharsets.UTF_8);
    }

    private void sendBytes(
            HttpExchange exchange,
            int statusCode,
            byte[] contenuto,
            String contentType) throws IOException {

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, contenuto.length);

        try (var output = exchange.getResponseBody()) {
            output.write(contenuto);
        }
    }
}
