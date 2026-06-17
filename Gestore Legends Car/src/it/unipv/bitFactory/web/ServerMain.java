package it.unipv.bitFactory.web;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.dao.csv.FileLegendsDAO;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) {

        try {
            // DAO utilizzato dal controller delle sessioni
            LegendsDAO dao =
                    new FileLegendsDAO("data/legends.csv");

            // Il controller delle sessioni richiede il DAO
            GestioneSessioniController sessioniController =
                    new GestioneSessioniController(dao);

            // Controller degli altri casi d'uso
            GestioneMagazzinoController magazzinoController =
                    new GestioneMagazzinoController();

            GestionePrenotazioniController prenotazioniController =
                    new GestionePrenotazioniController();

            // Generatore delle pagine HTML
            HtmlRenderer renderer = new HtmlRenderer();

            // Creazione del server sulla porta 8080 con 8 thread
            BitFactoryWebServer server =
                    new BitFactoryWebServer(
                            8082,
                            8,
                            sessioniController,
                            magazzinoController,
                            prenotazioniController,
                            renderer
                    );

            // Arresta correttamente il server alla chiusura
            Runtime.getRuntime().addShutdownHook(
                    new Thread(server::arresta)
            );
          
            server.avvia();

        } catch (Exception e) {
            System.err.println(
                    "Errore durante l'avvio del server: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}