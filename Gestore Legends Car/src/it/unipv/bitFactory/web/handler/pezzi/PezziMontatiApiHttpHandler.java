package it.unipv.bitFactory.web.handler.pezzi;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.web.handler.BaseHttpHandler;

public final class PezziMontatiApiHttpHandler
        extends BaseHttpHandler {

    private final GestioneMagazzinoController controller;

    public PezziMontatiApiHttpHandler(
            GestioneMagazzinoController controller
    ) {
        this.controller = Objects.requireNonNull(
                controller
        );
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        if (!isGet(exchange)) {
            exchange.getResponseHeaders().set(
                    "Allow",
                    "GET"
            );

            sendJson(
                    exchange,
                    405,
                    "{\"errore\":\"Metodo non supportato\"}"
            );
            return;
        }

        try {
            Map<String, String> parametri =
                    parametriQuery(exchange);

            String idMacchina =
                    parametri.get("idMacchina");

            String tipoTestuale =
                    parametri.get("tipo");

            if (idMacchina == null
                    || idMacchina.isBlank()) {
                throw new IllegalArgumentException(
                        "Parametro obbligatorio mancante: idMacchina"
                );
            }

            if (tipoTestuale == null
                    || tipoTestuale.isBlank()) {
                throw new IllegalArgumentException(
                        "Parametro obbligatorio mancante: tipo"
                );
            }

            TipoPezzo tipo = TipoPezzo.valueOf(
                    tipoTestuale
                            .trim()
                            .toUpperCase(Locale.ROOT)
            );

            List<Pezzo> pezzi =
                    controller.trovaPezziMontati(
                            idMacchina,
                            tipo
                    );

            sendJson(
                    exchange,
                    200,
                    creaJson(pezzi)
            );

        } catch (IllegalArgumentException e) {
            sendJson(
                    exchange,
                    400,
                    "{\"errore\":\""
                            + escape(e.getMessage())
                            + "\"}"
            );

        } catch (RuntimeException e) {
            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    "{\"errore\":\"Impossibile leggere i pezzi montati\"}"
            );
        }
    }

    private String creaJson(List<Pezzo> pezzi) {
        return pezzi.stream()
                .map(this::pezzoJson)
                .collect(
                        Collectors.joining(
                                ",",
                                "[",
                                "]"
                        )
                );
    }

    private String pezzoJson(Pezzo pezzo) {
        return "{"
                + "\"idPezzo\":\""
                + escape(pezzo.getIdPezzo())
                + "\","
                + "\"tipoPezzo\":\""
                + escape(pezzo.getTipo().name())
                + "\","
                + "\"kmAttuali\":"
                + pezzo.getKmAttuali()
                + ","
                + "\"kmMax\":"
                + pezzo.getKmMax()
                + ","
                + "\"tempoAttuale\":"
                + pezzo.getTempoAttuale()
                + ","
                + "\"tempoMax\":"
                + pezzo.getTempoMax()
                + "}";
    }

    private Map<String, String> parametriQuery(
            HttpExchange exchange
    ) {
        Map<String, String> parametri =
                new LinkedHashMap<>();

        String query =
                exchange.getRequestURI().getRawQuery();

        if (query == null || query.isBlank()) {
            return parametri;
        }

        for (String coppia : query.split("&")) {
            String[] parti = coppia.split("=", 2);

            String nome = decodifica(parti[0]);

            String valore = parti.length == 2
                    ? decodifica(parti[1])
                    : "";

            parametri.put(nome, valore);
        }

        return parametri;
    }

    private String decodifica(String valore) {
        return URLDecoder.decode(
                valore,
                StandardCharsets.UTF_8
        );
    }

    private String escape(String valore) {
        return valore == null
                ? ""
                : valore
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");
    }
}