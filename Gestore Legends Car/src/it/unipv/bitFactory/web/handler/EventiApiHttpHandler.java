package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.model.prenotazioni.Evento;
import it.unipv.bitFactory.service.GestoreEventi;

public final class EventiApiHttpHandler
        extends BaseHttpHandler {

    private final GestoreEventi gestoreEventi;

    public EventiApiHttpHandler(
            GestoreEventi gestoreEventi) {

        this.gestoreEventi = Objects.requireNonNull(
                gestoreEventi,
                "Il gestore degli eventi non può essere null"
        );
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        /*
         * Questa rotta accetta soltanto richieste GET.
         */
        if (!isGet(exchange)) {

            exchange.getResponseHeaders().set(
                    "Allow",
                    "GET"
            );

            sendJson(
                    exchange,
                    405,
                    """
                    {
                        "errore": "Metodo HTTP non consentito"
                    }
                    """
            );

            return;
        }

        try {

            /*
             * Recupera gli eventi dal database attraverso:
             *
             * GestoreEventi
             *      ↓
             * EventoDAO
             *      ↓
             * SqliteEventoDAO
             */
            List<Evento> eventi =
                    gestoreEventi.getEventi();

            String json =
                    convertiEventiInJson(eventi);

            sendJson(
                    exchange,
                    200,
                    json
            );

        } catch (RuntimeException e) {

            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    """
                    {
                        "errore": "Impossibile caricare gli eventi"
                    }
                    """
            );
        }
    }

    private String convertiEventiInJson(
            List<Evento> eventi) {

        StringBuilder json =
                new StringBuilder();

        json.append("[");

        for (int i = 0; i < eventi.size(); i++) {

            Evento evento = eventi.get(i);

            if (i > 0) {
                json.append(",");
            }

            json.append("{");

            json.append("\"nomeEvento\":\"")
                    .append(
                            escapeJson(
                                    evento.getNomeEvento()
                            )
                    )
                    .append("\",");

            json.append("\"dataEvento\":\"")
                    .append(
                            escapeJson(
                                    evento.getDataEvento()
                            )
                    )
                    .append("\",");

            json.append("\"postiDisponibili\":")
                    .append(
                            evento.getPostiDisponibili()
                    );

            json.append("}");
        }

        json.append("]");

        return json.toString();
    }

    private String escapeJson(String valore) {

        if (valore == null) {
            return "";
        }

        return valore
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}