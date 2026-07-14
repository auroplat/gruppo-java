package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.model.persona.Cliente;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class PrenotazioniHttpHandler
        extends BaseHttpHandler {

    private final GestionePrenotazioniController controller;
    private final HtmlRenderer renderer;

    public PrenotazioniHttpHandler(
            GestionePrenotazioniController controller,
            HtmlRenderer renderer) {

        this.controller = Objects.requireNonNull(
                controller,
                "Il controller delle prenotazioni non può essere null"
        );

        this.renderer = Objects.requireNonNull(
                renderer,
                "Il renderer HTML non può essere null"
        );
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        if (!isPost(exchange)) {

            exchange.getResponseHeaders().set(
                    "Allow",
                    "POST"
            );

            sendHtml(
                    exchange,
                    405,
                    renderer.renderErrore(
                            "Questa pagina accetta soltanto "
                                    + "richieste POST."
                    )
            );

            return;
        }

        try {

            Map<String, String> parametri =
                    leggiParametriForm(exchange);

            String operazione =
                    parametroObbligatorio(
                            parametri,
                            "operazione"
                    ).toLowerCase(Locale.ROOT);

            switch (operazione) {

                case "prenota" ->
                        gestisciPrenotazione(
                                exchange,
                                parametri
                        );

                case "annulla" ->
                        gestisciAnnullamento(
                                exchange,
                                parametri
                        );

                default ->
                        throw new IllegalArgumentException(
                                "Operazione non riconosciuta: "
                                        + operazione
                        );
            }

        } catch (DateTimeParseException e) {

            sendHtml(
                    exchange,
                    400,
                    renderer.renderErrore(
                            "La data di nascita inserita "
                                    + "non è valida."
                    )
            );

        } catch (IllegalArgumentException e) {

            sendHtml(
                    exchange,
                    400,
                    renderer.renderErrore(
                            e.getMessage()
                    )
            );

        } catch (RuntimeException e) {

            e.printStackTrace();

            sendHtml(
                    exchange,
                    500,
                    renderer.renderErrore(
                            "Si è verificato un errore durante "
                                    + "la gestione della prenotazione."
                    )
            );
        }
    }

    private void gestisciPrenotazione(
            HttpExchange exchange,
            Map<String, String> parametri)
            throws IOException {

        String nomeEvento =
                parametroObbligatorio(
                        parametri,
                        "evento"
                );

        String nome =
                parametroObbligatorio(
                        parametri,
                        "nome"
                );

        String cognome =
                parametroObbligatorio(
                        parametri,
                        "cognome"
                );

        String dataNascitaTesto =
                parametroObbligatorio(
                        parametri,
                        "dataNascita"
                );

        String email =
                parametroObbligatorio(
                        parametri,
                        "email"
                );

        String telefono =
                parametroObbligatorio(
                        parametri,
                        "telefono"
                );

        String patenteValida =
                parametri.get("patenteValida");

        if (!"true".equalsIgnoreCase(patenteValida)) {

            throw new IllegalArgumentException(
                    "Devi dichiarare di possedere "
                            + "una patente di guida valida."
            );
        }

        if (!telefono.matches("[0-9]{6,15}")) {

            throw new IllegalArgumentException(
                    "Il numero di telefono deve contenere "
                            + "da 6 a 15 cifre, incluso "
                            + "il prefisso internazionale."
            );
        }

        LocalDate dataNascita =
                LocalDate.parse(dataNascitaTesto);

        Cliente cliente = new Cliente(
                nome,
                cognome,
                dataNascita,
                email,
                "+" + telefono
        );

        String messaggio =
                controller.prenota(
                        cliente,
                        nomeEvento
                );

        boolean successo =
                iniziaCon(
                        messaggio,
                        "prenotazione completata"
                );

        sendHtml(
                exchange,
                successo ? 201 : 400,
                renderer.renderEsitoPrenotazione(
                        successo,
                        messaggio,
                        nomeEvento
                )
        );
    }

    private void gestisciAnnullamento(
            HttpExchange exchange,
            Map<String, String> parametri)
            throws IOException {

        String nomeEvento =
                parametroObbligatorio(
                        parametri,
                        "evento"
                );

        String email =
                parametroObbligatorio(
                        parametri,
                        "email"
                ).toLowerCase(Locale.ROOT);

        if (!email.contains("@")) {

            throw new IllegalArgumentException(
                    "L'indirizzo email inserito non è valido."
            );
        }

        String messaggio =
                controller.annullaPrenotazione(
                        email,
                        nomeEvento
                );

        boolean successo =
                iniziaCon(
                        messaggio,
                        "prenotazione annullata"
                );

        sendHtml(
                exchange,
                successo ? 200 : 404,
                renderer.renderEsitoPrenotazione(
                        successo,
                        messaggio,
                        nomeEvento
                )
        );
    }

    private boolean iniziaCon(
            String messaggio,
            String prefisso) {

        return messaggio != null
                && messaggio
                .toLowerCase(Locale.ROOT)
                .startsWith(prefisso);
    }
}