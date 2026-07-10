package it.unipv.bitFactory.service;

import it.unipv.bitFactory.dao.csv.EventoDAO;
import it.unipv.bitFactory.model.prenotazioni.Evento;
import java.util.Collections;
import java.util.List;


public class GestoreEventi {

    private List<Evento> eventi;

    public GestoreEventi() {

        EventoDAO dao = new EventoDAO();
        eventi = dao.caricaEventi("eventi.csv");
    }

    public List<Evento> getEventi() {

        return Collections.unmodifiableList(eventi);
    }

    public Evento cercaEvento(String nome) {

        for (Evento e : eventi) {

            if (e.getNomeEvento().equalsIgnoreCase(nome)) {
                return e;
            }
        }

        return null;
    }
}