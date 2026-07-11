package it.unipv.bitFactory.dao;

public class EventoDAOException extends Exception {

    public EventoDAOException(String messaggio) {
        super(messaggio);
    }

    public EventoDAOException(String messaggio, Throwable causa) {
        super(messaggio, causa);
    }
}