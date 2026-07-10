package it.unipv.bitFactory.prenotazioni;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GestorePrenotazioni {

    private static final GestorePrenotazioni instance =
            new GestorePrenotazioni();

    private List<Prenotazione> prenotazioni;

    private GestorePrenotazioni() {

        prenotazioni = new ArrayList<>();
    }

    public static GestorePrenotazioni getInstance() {

        return instance;
    }

    public void aggiungiPrenotazione(Prenotazione prenotazione) {

        prenotazioni.add(prenotazione);
    }

    public List<Prenotazione> getPrenotazioni() {

        return Collections.unmodifiableList(prenotazioni);
    }

    public void rimuoviPrenotazione(Prenotazione prenotazione) {

        prenotazioni.remove(prenotazione);
    }

    public Prenotazione cercaPrenotazione(String nomeCliente,
                                          String nomeEvento) {

        for (Prenotazione p : prenotazioni) {

            if (p.getNomeCliente().equalsIgnoreCase(nomeCliente)
                    && p.getNomeEvento().equalsIgnoreCase(nomeEvento)) {

                return p;
            }
        }

        return null;
    }
}