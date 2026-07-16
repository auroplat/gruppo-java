package it.unipv.bitFactory.controller;

import it.unipv.bitFactory.model.persona.Cliente;
import it.unipv.bitFactory.service.ServizioPrenotazioni;

public final class GestionePrenotazioniController {

    private final ServizioPrenotazioni sistema;

    public GestionePrenotazioniController(ServizioPrenotazioni sistema) {

        if (sistema == null) {
            throw new IllegalArgumentException("Il sistema prenotazioni non può essere null");
        }

        this.sistema = sistema;
    }

    public String prenota(Cliente cliente, String nomeEvento) {

        if (cliente == null) {return "Cliente non valido.";}

        if (nomeEvento == null || nomeEvento.isBlank()) {return "Evento non valido.";}

        return sistema.effettuaPrenotazione(cliente, nomeEvento);
    }

    public String annullaPrenotazione(String emailCliente, String nomeEvento) {

        if (emailCliente == null || emailCliente.isBlank()) {return "Email non valida.";}

        if (nomeEvento == null || nomeEvento.isBlank()) {return "Evento non valido.";}

        return sistema.annullaPrenotazione(emailCliente, nomeEvento);
    }
}