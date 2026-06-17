package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class BaseHttpHandler implements HttpHandler {

    protected boolean isGet(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(
                exchange.getRequestMethod()
        );
    }

    protected boolean isPost(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(
                exchange.getRequestMethod()
        );
    }

    protected void sendHtml(
            HttpExchange exchange,
            int statusCode,
            String html) throws IOException {

        byte[] response =
                html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
                statusCode,
                response.length
        );

        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    protected void redirect(
            HttpExchange exchange,
            String indirizzo) throws IOException {

        exchange.getResponseHeaders().set(
                "Location",
                indirizzo
        );

        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }
}