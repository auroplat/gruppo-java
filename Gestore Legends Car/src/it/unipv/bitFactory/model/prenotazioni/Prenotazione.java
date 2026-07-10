//Creazione della prenotazione 


package it.unipv.bitFactory.prenotazioni;

import it.unipv.bitFactory.veicoli.Legends;

public class Prenotazione {

    private String nomeCliente;
    private String nomeEvento;
    private Legends autoPrenotata;

    public Prenotazione(String nomeCliente, String nomeEvento, Legends autoPrenotata) {

        this.nomeCliente = nomeCliente;
        this.nomeEvento = nomeEvento;
        this.autoPrenotata = autoPrenotata;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public String getNomeEvento() {
        return nomeEvento;
    }

    public Legends getAutoPrenotata() {
        return autoPrenotata;
    }

    @Override
    public String toString() {

        return "Cliente: " + nomeCliente +
                " | Evento: " + nomeEvento +
                " | Auto: " + autoPrenotata.getId();
    }
}