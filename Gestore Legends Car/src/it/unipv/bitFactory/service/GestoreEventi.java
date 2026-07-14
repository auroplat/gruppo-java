package it.unipv.bitFactory.service;

import java.util.List;
import java.util.Objects;

import it.unipv.bitFactory.dao.interfacce.EventoDAO;
import it.unipv.bitFactory.model.prenotazioni.Evento;

public final class GestoreEventi {

    private final EventoDAO eventoDAO;

    public GestoreEventi(EventoDAO eventoDAO) {

        this.eventoDAO = Objects.requireNonNull( eventoDAO, "Il DAO degli eventi non può essere null" ); }

    public List<Evento> getEventi() {

        return List.copyOf( eventoDAO.caricaEventi());
    }

    public Evento cercaEvento(String nomeEvento) {

        if (nomeEvento == null || nomeEvento.isBlank()) { return null; }

        return eventoDAO.cercaEvento( nomeEvento.trim() );
    }
}