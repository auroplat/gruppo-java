package it.unipv.bitFactory.web;

import java.nio.file.Path;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.controller.GestioneSessioniController;

import it.unipv.bitFactory.dao.ClienteDAO;
import it.unipv.bitFactory.dao.EventoDAO;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.dao.PrenotazioneDAO;

import it.unipv.bitFactory.dao.sqlite.SqliteClienteDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteEventoDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteLegendsDAO;
import it.unipv.bitFactory.dao.sqlite.SqlitePrenotazioneDAO;

import it.unipv.bitFactory.service.GestoreEventi;
import it.unipv.bitFactory.service.SistemaNotifiche;
import it.unipv.bitFactory.service.SistemaPrenotazioni;

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

            String percorsoDatabase =
                    databasePath.toString();

            /*
             * PARTE SESSIONI
             * Rimane invariata.
             */
            LegendsDAO legendsDAO =
                    new SqliteLegendsDAO(
                            percorsoDatabase
                    );

            GestioneSessioniController sessioniController =
                    new GestioneSessioniController(
                            legendsDAO
                    );

            /*
             * PARTE MAGAZZINO
             * Rimane invariata.
             */
            GestioneMagazzinoController magazzinoController =
                    new GestioneMagazzinoController();

            /*
             * DAO PER EVENTI E PRENOTAZIONI
             */
            EventoDAO eventoDAO =
                    new SqliteEventoDAO(
                            percorsoDatabase
                    );

            ClienteDAO clienteDAO =
                    new SqliteClienteDAO(
                            percorsoDatabase
                    );

            PrenotazioneDAO prenotazioneDAO =
                    new SqlitePrenotazioneDAO(
                            percorsoDatabase
                    );

            /*
             * SERVIZIO USATO PER MOSTRARE
             * GLI EVENTI NELLA PAGINA WEB.
             */
            GestoreEventi gestoreEventi =
                    new GestoreEventi(
                            eventoDAO
                    );

            /*
             * SERVIZIO CHE GESTISCE
             * LE PRENOTAZIONI.
             */
            SistemaNotifiche sistemaNotifiche =
                    new SistemaNotifiche();

            SistemaPrenotazioni sistemaPrenotazioni =
                    new SistemaPrenotazioni(
                            clienteDAO,
                            eventoDAO,
                            prenotazioneDAO,
                            sistemaNotifiche
                    );

            GestionePrenotazioniController
                    prenotazioniController =
                    new GestionePrenotazioniController(
                            sistemaPrenotazioni
                    );

            HtmlRenderer renderer =
                    new HtmlRenderer();

            BitFactoryWebServer server =
                    new BitFactoryWebServer(
                            8082,
                            8,
                            sessioniController,
                            magazzinoController,
                            prenotazioniController,
                            gestoreEventi,
                            renderer
                    );

            Runtime.getRuntime().addShutdownHook(
                    new Thread(server::arresta)
            );

            System.out.println(
                    "Database SQLite: "
                            + databasePath
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