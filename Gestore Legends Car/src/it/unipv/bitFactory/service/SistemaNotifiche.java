package it.unipv.bitFactory.service;

import it.unipv.bitFactory.model.persona.Cliente;
import it.unipv.bitFactory.model.prenotazioni.Evento;

public class SistemaNotifiche {

    public void inviaConferma(Cliente cliente, Evento evento) {

        System.out.println(
                "Notifica inviata a " +
                cliente.getEmail() +
                " per l'evento " +
                evento.getNomeEvento()
        );
    }

    public void inviaAnnullamento(Cliente cliente, Evento evento) {

        System.out.println(
                "Annullamento inviato a " +
                cliente.getEmail() +
                " per l'evento " +
                evento.getNomeEvento()
        );
    }
}
