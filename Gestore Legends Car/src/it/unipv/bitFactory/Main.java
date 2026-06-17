package it.unipv.bitFactory;

import java.util.List;

import it.unipv.bitFactory.adapter.SessioneEsternaAdapter;
import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.dao.csv.FileLegendsDAO;
import it.unipv.bitFactory.external.SessioneEsterna;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.model.sessioni.Gara;
import it.unipv.bitFactory.model.sessioni.Sessione;
import it.unipv.bitFactory.model.sessioni.Test;
import it.unipv.bitFactory.model.veicoli.Legends;

public class Main {

    public static void main(String[] args) {

        LegendsDAO dao = new FileLegendsDAO("data/legends.csv");
        GestioneSessioniController controller = new GestioneSessioniController(dao);

        // Creazione macchina principale
        Legends legends = new Legends("L001");

        legends.montaPezzo(TipoPezzo.MOTORE, 1000.0, 500);
        legends.montaPezzo(TipoPezzo.FRENO, 300.0, 100);
        legends.montaPezzo(TipoPezzo.RUOTA, 200.0, 80);
        legends.montaPezzo(TipoPezzo.VOLANTE, 1500.0, 800);

        dao.salva(legends);

        // Sessione normale
        Sessione test = new Test("Monza", 50.0, 30, "Test assetto");
        controller.registraSessione("L001", test);

        // Sessione selettiva
        Sessione gara = new Gara("Imola", 70.5, 45, 2);

        controller.registraSessioneSelettiva(
                "L001",
                gara,
                List.of(TipoPezzo.MOTORE, TipoPezzo.FRENO, TipoPezzo.RUOTA)
        );

        // Sessione esterna adattata
        SessioneEsterna sessioneEsterna = new SessioneEsterna(
                "Mugello",
                40.0,
                25,
                "RACE"
        );

        Sessione sessioneAdattata = new SessioneEsternaAdapter(sessioneEsterna);
        controller.registraSessione("L001", sessioneAdattata);

        // Caricamento e stampa macchina aggiornata
        Legends caricata = dao.trovaPerId("L001")
                .orElseThrow(() -> new IllegalArgumentException("Macchina non trovata"));

        System.out.println("Macchina caricata:");
        System.out.println("ID: " + caricata.getId());
        System.out.println("KM totali: " + caricata.getKmTotali());

        for (Pezzo pezzo : caricata.getTuttiPezzi()) {
            System.out.println(pezzo);

            if (pezzo.daSostituire()) {
                System.out.println("ATTENZIONE: " + pezzo.getTipo() + " da sostituire");
            }
        }
    }
}