package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class SessioniHttpHandler
        extends BaseHttpHandler {

    private final GestioneSessioniController controller;
    private final HtmlRenderer renderer;

    public SessioniHttpHandler(
            GestioneSessioniController controller,
            HtmlRenderer renderer) {

        this.controller =
                Objects.requireNonNull(controller);

        this.renderer =
                Objects.requireNonNull(renderer);
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        if (isGet(exchange)) {
            mostraSessioni(exchange);
            return;
        }

        if (isPost(exchange)) {
            creaSessione(exchange);
            return;
        }

        sendHtml(
                exchange,
                405,
                renderer.renderErrore(
                        "Metodo HTTP non supportato"
                )
        );
    }

    private void mostraSessioni(
            HttpExchange exchange) throws IOException {

        // Successivamente:
        // var sessioni = controller.elencaSessioni();
        // String html = renderer.renderSessioni(sessioni);

        sendHtml(
                exchange,
                200,
                "<h1>Gestione sessioni</h1>"
        );
    }

    private void creaSessione(
            HttpExchange exchange) throws IOException {

        // Successivamente:
        // leggere i dati inviati dal form
        // controller.programmaSessione(...);

        redirect(exchange, "/sessioni");
    }
}