package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class PrenotazioniHttpHandler
        extends BaseHttpHandler {

    private final GestionePrenotazioniController controller;
    private final HtmlRenderer renderer;

    public PrenotazioniHttpHandler(
            GestionePrenotazioniController controller,
            HtmlRenderer renderer) {

        this.controller =
                Objects.requireNonNull(controller);

        this.renderer =
                Objects.requireNonNull(renderer);
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        if (!isGet(exchange)) {
            sendHtml(
                    exchange,
                    405,
                    renderer.renderErrore(
                            "Metodo HTTP non supportato"
                    )
            );
            return;
        }

        sendHtml(
                exchange,
                200,
                "<h1>Gestione prenotazioni</h1>"
        );
    }
}