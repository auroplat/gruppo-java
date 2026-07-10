package it.unipv.bitFactory.service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import it.unipv.bitFactory.model.prenotazioni.Evento;
import it.unipv.bitFactory.adapter.AdapterEventiEsterni;

public class GestoreEventiEsterni {

    private List<Evento> eventi;
    private AdapterEventiEsterni adapter;

    public GestoreEventiEsterni() {

        eventi = new ArrayList<>();
        adapter = new AdapterEventiEsterni();
    }

    public void caricaEventiDaCSVEsterno(String percorsoFile) {

        try (BufferedReader br =
                     new BufferedReader(new FileReader(percorsoFile))) {

            String riga;

            br.readLine(); // salta header

            while ((riga = br.readLine()) != null) {

                Evento evento = adapter.convertiRiga(riga);
                eventi.add(evento);
            }

        } catch (IOException e) {

            System.out.println("Errore lettura file eventi esterni");
        }
    }

    public List<Evento> getEventi() {
        return eventi;
    }
}