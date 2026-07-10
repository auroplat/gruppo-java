package it.unipv.bitFactory.controller;

import it.unipv.bitFactory.prenotazioni.Cliente;
import it.unipv.bitFactory.prenotazioni.Evento;
import it.unipv.bitFactory.prenotazioni.SistemaPrenotazioni;
import it.unipv.bitFactory.veicoli.Legends;

public class PrenotazioneController {

    private SistemaPrenotazioni sistema;

    public PrenotazioneController() {

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