package it.unipv.bitFactory.controller;

import java.util.List;
import java.util.Objects;

import it.unipv.bitFactory.external.SessioneEsterna;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.service.ServizioSessioni;

public final class GestioneSessioniController {

    private final ServizioSessioni sessioniService;

    public GestioneSessioniController(ServizioSessioni sessioniService) {
    	
        this.sessioniService = Objects.requireNonNull(sessioniService, "Il service delle sessioni non può essere null");
    }

    public void registraSessione( String idMacchina, Sessione sessione) {
        sessioniService.registraSessione(idMacchina, sessione);
    }

    public List<String> elencaIdMacchine() {
        return sessioniService.elencaIdMacchine();
    }

    public void registraSessioneEsterna(String idMacchina, SessioneEsterna sessioneEsterna) {
        
    	sessioniService.registraSessioneEsterna(idMacchina, sessioneEsterna);
    }
}