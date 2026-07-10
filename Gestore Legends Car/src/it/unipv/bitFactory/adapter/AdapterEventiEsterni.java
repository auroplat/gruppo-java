package it.unipv.bitFactory.adapter;

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
