package it.unipv.bitFactory.web.handler.sessioni;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.web.handler.BaseHttpHandler;

public final class MacchineApiHttpHandler extends BaseHttpHandler {

    private final GestioneSessioniController controller;

    public MacchineApiHttpHandler(GestioneSessioniController controller) {

        this.controller = Objects.requireNonNull(controller);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!isGet(exchange)) {
            exchange.getResponseHeaders().set("Allow", "GET");
            sendJson(exchange, 405, "{\"errore\":\"Metodo non supportato\"}");
            return;
        }

        try {
            List<String> ids = controller.elencaIdMacchine();
            sendJson(exchange, 200, creaJson(ids));
        } catch (RuntimeException e) {
            e.printStackTrace();
            sendJson(
                    exchange,
                    500,
                    "{\"errore\":\"Impossibile leggere le macchine\"}"
            );
        }
    }

    private String creaJson(List<String> ids) {
        return ids.stream()
                .map(this::stringaJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private String stringaJson(String valore) {
        String escaped = valore
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");

        return "\"" + escaped + "\"";
    }
}
