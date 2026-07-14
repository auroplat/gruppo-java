
package it.unipv.bitFactory.controller;

import it.unipv.bitFactory.model.persona.Addetto;
import it.unipv.bitFactory.service.ServizioAutenticazione;

public class LoginController {

    private final ServizioAutenticazione servizioAutenticazione;

    public LoginController(ServizioAutenticazione servizioAutenticazione) {

        if (servizioAutenticazione == null) {
            throw new IllegalArgumentException("Il servizio di autenticazione non può essere nullo");
        }

        this.servizioAutenticazione = servizioAutenticazione;
    }

    public Addetto login(String username, String password) {
        return servizioAutenticazione.autentica(username, password);
    }
}

