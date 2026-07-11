package it.unipv.bitFactory.dao;

import it.unipv.bitFactory.model.prenotazioni.Evento;

import java.util.List;

public interface EventoDAO {

    List<Evento> caricaEventi() throws EventoDAOException;

    Evento cercaEvento(String nome) throws EventoDAOException;

    boolean aggiungiEvento(Evento evento) throws EventoDAOException;

    boolean eliminaEvento(String nome) throws EventoDAOException;

    boolean aggiornaPosti(String nome, int nuoviPosti) throws EventoDAOException;
}