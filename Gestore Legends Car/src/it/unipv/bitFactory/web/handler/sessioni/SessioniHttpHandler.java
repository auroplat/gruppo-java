package it.unipv.bitFactory.web.handler.sessioni;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.SessioneFactory;
import it.unipv.bitFactory.model.sessioni.TipoSessione;
import it.unipv.bitFactory.web.handler.BaseHttpHandler;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class SessioniHttpHandler
        extends BaseHttpHandler {

    private final GestioneSessioniController controller;
    private final HtmlRenderer renderer;

    public SessioniHttpHandler(GestioneSessioniController controller, HtmlRenderer renderer) {

        this.controller =
                Objects.requireNonNull(controller);

        this.renderer =
                Objects.requireNonNull(renderer);
    }

    @Override
    public void handle(
            HttpExchange exchange) throws IOException {

        if (isGet(exchange)) {
            mostraSessioni(exchange);
            return;
        }

        if (isPost(exchange)) {
            creaSessione(exchange);
            return;
        }

        exchange.getResponseHeaders()
                .set("Allow", "GET, POST");

        sendHtml(
                exchange,
                405,
                renderer.renderErrore(
                        "Metodo HTTP non supportato"
                )
        );
    }

    private void mostraSessioni(HttpExchange exchange) throws IOException {

        sendResource(
                exchange,
                "/web/sessioni.html",
                "text/html; charset=UTF-8"
        );
    }

    private void creaSessione(HttpExchange exchange) throws IOException {

        try {
            Map<String, String> form = leggiParametriForm(exchange);

            String idMacchina =parametroObbligatorio(form, "macchina");

            String tipo = parametroObbligatorio(form, "tipoSessione");

            String luogo = parametroObbligatorio(form, "luogo");

            double kmPercorsi = Double.parseDouble(parametroObbligatorio(form, "kmPercorsi").replace(',', '.'));

            int tempoPassato = Integer.parseInt(parametroObbligatorio(form, "tempoPassato"));

            Sessione sessione =
                    creaOggettoSessione(
                            form,
                            tipo,
                            luogo,
                            kmPercorsi,
                            tempoPassato
                    );

            controller.registraSessione(
                    idMacchina,
                    sessione
            );

            redirect(
                    exchange,
                    "/sessioni?success=1"
            );

        } catch (IllegalArgumentException e) {
            sendHtml(
                    exchange,
                    400,
                    renderer.renderErrore(e.getMessage())
            );

        } catch (RuntimeException e) {
            e.printStackTrace();

            sendHtml(
                    exchange,
                    500,
                    renderer.renderErrore(
                            "Errore durante la registrazione "
                            + "della sessione"
                    )
            );
        }
    }

    private Sessione creaOggettoSessione(
            Map<String, String> form,
            String tipo,
            String luogo,
            double kmPercorsi,
            int tempoPassato) {

        TipoSessione tipoSessione = TipoSessione.daStringa(tipo);

        return SessioneFactory.crea(
                tipoSessione,
                luogo,
                kmPercorsi,
                tempoPassato,
                form.get("descrizione"),
                form.get("posizione")
        );
    }
}