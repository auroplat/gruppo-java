package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.model.sessioni.Gara;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.Test;
import it.unipv.bitFactory.thread.UsuraPezziThread;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class SessioniHttpHandler extends BaseHttpHandler {

    private final UsuraPezziThread usuraPezziThread;
    private final HtmlRenderer renderer;

    public SessioniHttpHandler(
            UsuraPezziThread usuraPezziThread,
            HtmlRenderer renderer) {

        this.usuraPezziThread = Objects.requireNonNull(usuraPezziThread);
        this.renderer = Objects.requireNonNull(renderer);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        System.out.printf(
                "[%s] %s %s%n",
                Thread.currentThread().getName(),
                exchange.getRequestMethod(),
                exchange.getRequestURI()
        );

        if (isGet(exchange)) {
            mostraSessioni(exchange);
            return;
        }

        if (isPost(exchange)) {
            creaSessione(exchange);
            return;
        }

        exchange.getResponseHeaders().set("Allow", "GET, POST");
        sendHtml(
                exchange,
                405,
                renderer.renderErrore("Metodo HTTP non supportato")
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

            String idMacchina = parametroObbligatorio(form, "macchina");
            String tipo = parametroObbligatorio(form, "tipoSessione");
            String luogo = parametroObbligatorio(form, "luogo");

            double kmPercorsi = Double.parseDouble(
                    parametroObbligatorio(form, "kmPercorsi")
                            .replace(',', '.')
            );

            int tempoPassato = Integer.parseInt(
                    parametroObbligatorio(form, "tempoPassato")
            );

            Sessione sessione = creaOggettoSessione(
                    form,
                    tipo,
                    luogo,
                    kmPercorsi,
                    tempoPassato
            );

            attendiCompletamento(
                    usuraPezziThread.inviaAggiornamento(
                            idMacchina,
                            sessione
                    )
            );

            redirect(exchange, "/sessioni?success=1");

        } catch (IllegalArgumentException e) {
            sendHtml(exchange, 400, renderer.renderErrore(e.getMessage()));

        } catch (RuntimeException e) {
            e.printStackTrace();
            sendHtml(
                    exchange,
                    500,
                    renderer.renderErrore(
                            "Errore durante la registrazione della sessione"
                    )
            );
        }
    }

    private void attendiCompletamento(
            java.util.concurrent.CompletableFuture<Void> risultato) {

        try {
            risultato.join();

        } catch (CompletionException e) {
            Throwable causa = e.getCause();

            if (causa instanceof IllegalArgumentException erroreInput) {
                throw erroreInput;
            }

            if (causa instanceof RuntimeException erroreRuntime) {
                throw erroreRuntime;
            }

            throw new RuntimeException(
                    "Scrittura della sessione non riuscita",
                    causa
            );
        }
    }

    private Sessione creaOggettoSessione(
            Map<String, String> form,
            String tipo,
            String luogo,
            double kmPercorsi,
            int tempoPassato) {

        if ("TEST".equalsIgnoreCase(tipo)) {
            return new Test(
                    luogo,
                    kmPercorsi,
                    tempoPassato,
                    parametroObbligatorio(form, "descrizione")
            );
        }

        if ("GARA".equalsIgnoreCase(tipo)) {
            int posizione = Integer.parseInt(
                    parametroObbligatorio(form, "posizione")
            );

            return new Gara(
                    luogo,
                    kmPercorsi,
                    tempoPassato,
                    posizione
            );
        }

        throw new IllegalArgumentException(
                "Tipo di sessione non valido: " + tipo
        );
    }
}
