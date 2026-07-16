package it.unipv.bitFactory.web.handler.login;

import java.io.IOException;
import java.util.Arrays;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import it.unipv.bitFactory.model.persona.Ruolo;
import it.unipv.bitFactory.service.ServizioSessioniLogin;
import it.unipv.bitFactory.web.ControlloAccesso;

public final class RuoloHttpHandler implements HttpHandler {

    private final HttpHandler handlerDelegato;
    private final ServizioSessioniLogin gestoreSessioniLogin;
    private final Ruolo[] ruoliConsentiti;

    public RuoloHttpHandler(
            HttpHandler handlerDelegato,
            ServizioSessioniLogin gestoreSessioniLogin,
            Ruolo... ruoliConsentiti) {

        if (handlerDelegato == null
                || gestoreSessioniLogin == null
                || ruoliConsentiti == null
                || ruoliConsentiti.length == 0
                || Arrays.stream(ruoliConsentiti)
                        .anyMatch(ruolo -> ruolo == null)) {

            throw new IllegalArgumentException(
                    "Handler, gestore sessioni e ruoli non possono essere nulli o vuoti"
            );
        }

        this.handlerDelegato = handlerDelegato;
        this.gestoreSessioniLogin = gestoreSessioniLogin;

        this.ruoliConsentiti = Arrays.copyOf(
                ruoliConsentiti,
                ruoliConsentiti.length
        );
    }

    @Override
    public void handle(HttpExchange exchange)
            throws IOException {

        if (!ControlloAccesso.consentiUnoDeiRuoli(
                exchange,
                gestoreSessioniLogin,
                ruoliConsentiti
        )) {
            return;
        }

        handlerDelegato.handle(exchange);
    }
}