package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

        /*
         * Il form deve inviare i dati tramite POST.
         */
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
                                    + "l'invio del form di prenotazione."
                    )
            );

            return;
        }

        try {
            /*
             * Legge tutti i dati inviati dal form HTML.
             */
            Map<String, String> parametri =
                    leggiParametriForm(exchange);

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

            /*
             * Una checkbox non selezionata non viene
             * inviata dal browser.
             */
            String patenteValida =
                    parametri.get("patenteValida");

            if (!"true".equalsIgnoreCase(patenteValida)) {
                throw new IllegalArgumentException(
                        "Devi dichiarare di possedere "
                                + "una patente di guida valida."
                );
            }

            /*
             * Nel campo HTML il simbolo + è mostrato
             * separatamente. Il valore ricevuto deve
             * quindi contenere soltanto cifre.
             */
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

            /*
             * Delega la prenotazione al controller.
             */
            String messaggio =
                    controller.prenota(
                            cliente,
                            nomeEvento
                    );

            boolean successo =
                    messaggio != null
                            && messaggio
                            .toLowerCase()
                            .startsWith(
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
                                    + "il salvataggio della prenotazione."
                    )
            );
        }
    }
}