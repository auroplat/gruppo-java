package it.unipv.bitFactory.dao;

import java.util.List;
import java.util.Optional;

import it.unipv.bitFactory.model.prenotazioni.Evento;

public interface EventoDAO {

    List<Evento> trovaTutti();

    Optional<Evento> trovaPerNome(String nomeEvento);

    void salva(Evento evento);

    boolean aggiornaPosti(String nomeEvento, int nuoviPosti);

    boolean elimina(String nomeEvento);
}