package it.unipv.bitFactory.web.handler.magazzino;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.web.handler.BaseHttpHandler;

public final class MagazzinoApiHttpHandler extends BaseHttpHandler {

    private final GestioneMagazzinoController controller;

    public MagazzinoApiHttpHandler(GestioneMagazzinoController controller) {
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
            List<VoceMagazzino> voci = controller.visualizzaMagazzino();
            sendJson(exchange, 200, creaJson(voci));
        } catch (RuntimeException e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"errore\":\"Impossibile leggere il magazzino\"}");
        }
    }

    private String creaJson(List<VoceMagazzino> voci) {
        return voci.stream()
                .map(this::voceJson)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String voceJson(VoceMagazzino voce) {
        return "{" +
                "\"idPezzo\":\"" + escape(voce.getIdPezzo()) + "\"," +
                "\"tipoPezzo\":\"" + escape(voce.getTipoPezzo().name()) + "\"," +
                "\"quantita\":" + voce.getQuantita() + "," +
                "\"statoDisponibilita\":\"" + escape(voce.getStatoDisponibilita().name()) + "\"" +
                "}";
    }

    private String escape(String valore) {
        return valore == null ? "" : valore
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
