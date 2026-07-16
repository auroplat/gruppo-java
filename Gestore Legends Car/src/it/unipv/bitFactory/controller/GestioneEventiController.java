package it.unipv.bitFactory.controller;

import java.util.List;

import it.unipv.bitFactory.model.prenotazioni.Evento;
import it.unipv.bitFactory.service.ServizioEventi;

public final class GestioneEventiController {

    private final ServizioEventi gestoreEventi;

    public GestioneEventiController(ServizioEventi gestoreEventi) {

        if (gestoreEventi == null) {
            throw new IllegalArgumentException("Il gestore degli eventi non può essere null");
        }

        this.gestoreEventi = gestoreEventi;
    }

    public List<Evento> elencaEventi() {
        return gestoreEventi.getEventi();
    }

    public Evento cercaEvento(String nomeEvento) {

        if (nomeEvento == null || nomeEvento.isBlank()) {
            return null;
        }

        return gestoreEventi.cercaEvento(nomeEvento);
    }

    public String creaEvento(
            String nomeEvento,
            String dataEvento,
            int postiDisponibili) {

        return gestoreEventi.creaEvento(
                nomeEvento,
                dataEvento,
                postiDisponibili
        );
    }
}
