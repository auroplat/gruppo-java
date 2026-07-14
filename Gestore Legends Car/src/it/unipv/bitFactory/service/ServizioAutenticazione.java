
package it.unipv.bitFactory.service;

import it.unipv.bitFactory.dao.interfacce.AddettoDAO;
import it.unipv.bitFactory.model.persona.Addetto;

public class ServizioAutenticazione {

    private final AddettoDAO addettoDAO;

    public ServizioAutenticazione(AddettoDAO addettoDAO) {

        if (addettoDAO == null) {
            throw new IllegalArgumentException("Il DAO degli addetti non può essere nullo");
        }

        this.addettoDAO = addettoDAO;
    }

    public Addetto autentica(String username, String password) {

        if (username == null || username.isBlank()) {return null;}
        if (password == null || password.isBlank()) {return null;}
        
        Addetto addetto = addettoDAO.trovaPerUsername(username.trim());
        if (addetto == null) {return null;}
        if (!addetto.getPassword().equals(password)) {return null;}

        return addetto;
    }
}
