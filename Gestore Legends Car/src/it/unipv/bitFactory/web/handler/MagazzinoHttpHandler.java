package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class MagazzinoHttpHandler
        extends BaseHttpHandler {

    private final GestioneMagazzinoController controller;
    private final HtmlRenderer renderer;

    public MagazzinoHttpHandler(
            GestioneMagazzinoController controller,
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
                "<h1>Gestione magazzino</h1>"
        );
    }
}