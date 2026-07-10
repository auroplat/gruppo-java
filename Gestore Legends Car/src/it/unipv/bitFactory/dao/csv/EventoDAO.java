package it.unipv.bitFactory.dao.csv;

import it.unipv.bitFactory.prenotazioni.Evento;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EventoDAO {

    public List<Evento> caricaEventi(String percorsoFile) {

        List<Evento> eventi = new ArrayList<>();

        try (BufferedReader lettore =
                     new BufferedReader(new FileReader(percorsoFile))) {

            lettore.readLine();

            String riga;

            while ((riga = lettore.readLine()) != null) {

                String[] dati = riga.split(",");

                eventi.add(new Evento(
                        dati[0],
                        dati[1],
                        Integer.parseInt(dati[2])
                ));
            }

        } catch (IOException e) {

            e.printStackTrace();
        }

        return eventi;
    }
}
