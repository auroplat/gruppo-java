package it.unipv.bitFactory.web.handler;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import it.unipv.bitFactory.model.persona.Ruolo;
import it.unipv.bitFactory.service.GestoreSessioniLogin;
import it.unipv.bitFactory.web.ControlloAccesso;

/**
 * Decoratore degli handler HTTP.
 *
 * Controlla che la richiesta appartenga a un addetto autenticato
 * con il ruolo richiesto e, solo in caso positivo, delega la
 * richiesta all'handler originale.
 */
public final class RuoloHttpHandler implements HttpHandler {

    private final HttpHandler handlerDelegato;
    private final GestoreSessioniLogin gestoreSessioniLogin;
    private final Ruolo ruoloRichiesto;

    public RuoloHttpHandler(
            HttpHandler handlerDelegato,
            GestoreSessioniLogin gestoreSessioniLogin,
            Ruolo ruoloRichiesto) {

        if (handlerDelegato == null
                || gestoreSessioniLogin == null
                || ruoloRichiesto == null) {

            throw new IllegalArgumentException(
                    "Handler, gestore sessioni e ruolo "
                            + "non possono essere null"
            );
        }

        this.handlerDelegato = handlerDelegato;
        this.gestoreSessioniLogin = gestoreSessioniLogin;
        this.ruoloRichiesto = ruoloRichiesto;
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        if (!ControlloAccesso.consentiRuolo(
                exchange,
                gestoreSessioniLogin,
                ruoloRichiesto
        )) {
            return;
        }

        handlerDelegato.handle(exchange);
    }
}
