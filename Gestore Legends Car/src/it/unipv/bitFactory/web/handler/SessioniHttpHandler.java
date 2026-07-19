package it.unipv.bitFactory.web.handler;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;

import com.sun.net.httpserver.HttpExchange;

import it.unipv.bitFactory.model.sessioni.Gara;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.Test;
import it.unipv.bitFactory.thread.Dispatcher;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class SessioniHttpHandler extends BaseHttpHandler {

    private final Dispatcher usuraPezziThread;
    private final HtmlRenderer renderer;

    public SessioniHttpHandler(
            Dispatcher usuraPezziThread,
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
            // 1. Estrae i dati dal form web
            Map<String, String> form = leggiParametriForm(exchange);

            String idMacchina = parametroObbligatorio(form, "macchina");
            String tipo = parametroObbligatorio(form, "tipoSessione");
            String luogo = parametroObbligatorio(form, "luogo");

            double kmPercorsi = Double.parseDouble(
                    parametroObbligatorio(form, "kmPercorsi").replace(',', '.')
            );

            int tempoPassato = Integer.parseInt(
                    parametroObbligatorio(form, "tempoPassato")
            );

            // 2. Crea l'oggetto Sessione (Gara o Test)
            Sessione sessione = creaOggettoSessione(
                    form,
                    tipo,
                    luogo,
                    kmPercorsi,
                    tempoPassato
            );

            // 3. LA GRANDE DIFFERENZA: Fire and Forget!
            // Inseriamo la richiesta nella coda del thread. Il thread farà notifyAll() 
            // e inizierà a lavorare, ma noi NON lo aspettiamo.
            usuraPezziThread.inviaAggiornamento(idMacchina, sessione);

            // 4. Rispondiamo immediatamente all'utente che l'operazione è presa in carico
            redirect(exchange, "/sessioni?success=1");

        } catch (IllegalArgumentException e) {
            // Gestione degli errori di input dell'utente (es. formattazione sbagliata)
            sendHtml(exchange, 400, renderer.renderErrore(e.getMessage()));

        } catch (RuntimeException e) {
            // Gestione di altri errori imprevisti
            e.printStackTrace();
            sendHtml(
                    exchange,
                    500,
                    renderer.renderErrore("Errore imprevisto durante la registrazione")
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
