package it.unipv.bitFactory.service;

import java.util.List;
import java.util.Objects;

import it.unipv.bitFactory.dao.interfacce.EventoDAO;
import it.unipv.bitFactory.model.prenotazioni.Evento;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class ServizioEventi {

    private final EventoDAO eventoDAO;

    public ServizioEventi(EventoDAO eventoDAO) {

        this.eventoDAO = Objects.requireNonNull(eventoDAO, "Il DAO degli eventi non può essere null");
    }

    public List<Evento> getEventi() {

        return List.copyOf(
                eventoDAO.caricaEventi()
        );
    }

    public Evento cercaEvento(String nomeEvento) {

        if (nomeEvento == null || nomeEvento.isBlank()) {
            return null;
        }

        return eventoDAO.cercaEvento(nomeEvento.trim());
    }
    
    public String creaEvento(String nomeEvento, String dataEvento, int postiDisponibili) {

        String nome = normalizzaNomeEvento(nomeEvento);
        String data = normalizzaDataEvento(dataEvento);

        if (postiDisponibili <= 0) {return "I posti disponibili devono essere maggiori di zero.";
        }

        if (eventoDAO.cercaEvento(nome) != null) {
            return "Esiste già un evento con questo nome.";
        }

        Evento evento = new Evento(nome, data, postiDisponibili);

        boolean creato = eventoDAO.aggiungiEvento(evento);

        if (!creato) {
            return "Impossibile creare l'evento nel database.";
        }

        return "Evento creato correttamente.";
    }

    private String normalizzaNomeEvento(String nomeEvento) {

        if (nomeEvento == null || nomeEvento.isBlank()) {
            throw new IllegalArgumentException("Il nome dell'evento non può essere vuoto.");
        }

        return nomeEvento.trim();
    }

    private String normalizzaDataEvento(String dataEvento) {

        if (dataEvento == null || dataEvento.isBlank()) {
            throw new IllegalArgumentException("La data dell'evento non può essere vuota.");
        }

        String data = dataEvento.trim();

        try {
            LocalDate.parse(data);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("La data dell'evento deve essere nel formato yyyy-MM-dd.");
        }

        return data;
    }
}