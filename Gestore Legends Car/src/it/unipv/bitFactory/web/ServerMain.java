package it.unipv.bitFactory.web;

import java.nio.file.Path;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteLegendsDAO;
import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) {
        try {
            Path databasePath = Path.of(
                    "data",
                    "database_bfactory.db"
            ).toAbsolutePath().normalize();

            LegendsDAO dao = new SqliteLegendsDAO(
                    databasePath.toString()
            );

            GestioneSessioniController sessioniController =
                    new GestioneSessioniController(dao);

            GestioneMagazzinoController magazzinoController =
                    new GestioneMagazzinoController();

            GestionePrenotazioniController prenotazioniController =
                    new GestionePrenotazioniController();

            HtmlRenderer renderer = new HtmlRenderer();

            BitFactoryWebServer server = new BitFactoryWebServer(
                    8082,
                    8,
                    sessioniController,
                    magazzinoController,
                    prenotazioniController,
                    renderer
            );

            Runtime.getRuntime().addShutdownHook(
                    new Thread(server::arresta)
            );

            System.out.println("Database SQLite: " + databasePath);
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