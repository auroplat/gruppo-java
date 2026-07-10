package it.unipv.bitFactory.controller;

import it.unipv.bitFactory.model.persona.Cliente;
import it.unipv.bitFactory.model.prenotazioni.Evento;
import it.unipv.bitFactory.service.SistemaPrenotazioni;
import it.unipv.bitFactory.model.veicoli.*;

public class GestionePrenotazioniController {

    private SistemaPrenotazioni sistema;

    public GestionePrenotazioniController() {

        sistema = new SistemaPrenotazioni();
    }

    public String prenota(Cliente cliente,
                          Evento evento,
                          Legends auto) {

        return sistema.effettuaPrenotazione(
                cliente,
                evento,
                auto
        );
    }

    public String annullaPrenotazione(Cliente cliente,
                                      Evento evento) {

        return sistema.annullaPrenotazione(
                cliente,
                evento
        );
    }
}