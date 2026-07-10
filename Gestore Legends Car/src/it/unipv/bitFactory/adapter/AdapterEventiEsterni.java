package it.unipv.bitFactory.adapter;
import it.unipv.bitFactory.model.prenotazioni.Evento;

public class AdapterEventiEsterni {

    public Evento convertiRiga(String riga) {

        String[] dati = riga.split(";");

        return new Evento(
                dati[0],                  // nomeEvento
                dati[1],                  // dataEvento
                Integer.parseInt(dati[2]) // postiDisponibili
        );
    }
}
