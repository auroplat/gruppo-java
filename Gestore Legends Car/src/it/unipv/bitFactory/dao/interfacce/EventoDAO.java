package it.unipv.bitFactory.dao.interfacce;

import java.util.List;

import it.unipv.bitFactory.model.prenotazioni.Evento;

public interface EventoDAO {

    List<Evento> caricaEventi();

    Evento cercaEvento(String nome);

    boolean aggiungiEvento(Evento evento);

    boolean eliminaEvento(String nome);

    boolean aggiornaPosti(String nome, int nuoviPosti);
}