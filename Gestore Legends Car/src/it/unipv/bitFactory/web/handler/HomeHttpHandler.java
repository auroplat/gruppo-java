package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class HomeHttpHandler
        extends BaseHttpHandler {

    private final HtmlRenderer renderer;

    public HomeHttpHandler(HtmlRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer);
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
                renderer.renderHome()
        );
    }
}