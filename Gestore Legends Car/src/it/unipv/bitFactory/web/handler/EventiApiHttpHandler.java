package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.model.prenotazioni.Evento;
import it.unipv.bitFactory.service.GestoreEventi;

public final class EventiApiHttpHandler
        extends BaseHttpHandler {

    private final GestoreEventi gestoreEventi;
    private final boolean accettaCreazione;

    public EventiApiHttpHandler(GestoreEventi gestoreEventi) {
        this(gestoreEventi, false);
    }

    public EventiApiHttpHandler(
            GestoreEventi gestoreEventi,
            boolean accettaCreazione) {

        this.gestoreEventi = Objects.requireNonNull(
                gestoreEventi,
                "Il gestore degli eventi non può essere null"
        );

        this.accettaCreazione = accettaCreazione;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        if (isGet(exchange)) {
            gestisciLettura(exchange);
            return;
        }

        if (isPost(exchange)) {
            gestisciCreazione(exchange);
            return;
        }

        exchange.getResponseHeaders().set(
                "Allow",
                accettaCreazione ? "GET, POST" : "GET"
        );

        sendJson(
                exchange,
                405,
                """
                {
                    "successo": false,
                    "messaggio": "Metodo HTTP non consentito"
                }
                """
        );
    }
    
    private void gestisciLettura(HttpExchange exchange) throws IOException {

        try {
            List<Evento> eventi = gestoreEventi.getEventi();

            String json = convertiEventiInJson(eventi);

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
                        "successo": false,
                        "messaggio": "Impossibile caricare gli eventi"
                    }
                    """
            );
        }
    }
    
    private void gestisciCreazione(HttpExchange exchange) throws IOException {

        if (!accettaCreazione) {
            exchange.getResponseHeaders().set("Allow", "GET");

            sendJson(
                    exchange,
                    405,
                    """
                    {
                        "successo": false,
                        "messaggio": "Questa rotta accetta soltanto la lettura degli eventi"
                    }
                    """
            );

            return;
        }

        try {
            Map<String, String> parametri = leggiParametriForm(exchange);

            String nomeEvento = parametroObbligatorio(parametri, "nomeEvento");
            String dataEvento = parametroObbligatorio(parametri, "dataEvento");

            int postiDisponibili = Integer.parseInt(
                    parametroObbligatorio(parametri, "postiDisponibili")
            );

            String messaggio = gestoreEventi.creaEvento(
                    nomeEvento,
                    dataEvento,
                    postiDisponibili
            );

            boolean successo = messaggio != null
                    && messaggio.toLowerCase().startsWith("evento creato");

            sendJson(
                    exchange,
                    successo ? 201 : 400,
                    """
                    {
                        "successo": %s,
                        "messaggio": "%s"
                    }
                    """.formatted(successo, escapeJson(messaggio))
            );

        } catch (NumberFormatException e) {
            sendJson(
                    exchange,
                    400,
                    """
                    {
                        "successo": false,
                        "messaggio": "I posti disponibili devono essere un numero intero."
                    }
                    """
            );

        } catch (IllegalArgumentException e) {
            sendJson(
                    exchange,
                    400,
                    """
                    {
                        "successo": false,
                        "messaggio": "%s"
                    }
                    """.formatted(escapeJson(e.getMessage()))
            );

        } catch (RuntimeException e) {
            e.printStackTrace();

            sendJson(
                    exchange,
                    500,
                    """
                    {
                        "successo": false,
                        "messaggio": "Errore durante la creazione dell'evento."
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