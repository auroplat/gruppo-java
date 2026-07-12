package it.unipv.bitFactory.web;

import java.nio.file.Path;

import it.unipv.bitFactory.controller.GestioneMagazzinoController;
import it.unipv.bitFactory.controller.GestionePrenotazioniController;
import it.unipv.bitFactory.controller.GestioneSessioniController;

import it.unipv.bitFactory.dao.ClienteDAO;
import it.unipv.bitFactory.dao.EventoDAO;
import it.unipv.bitFactory.dao.LegendsDAO;
import it.unipv.bitFactory.dao.MagazzinoDAO;
import it.unipv.bitFactory.dao.PrenotazioneDAO;
import it.unipv.bitFactory.dao.SessioneDAO;

import it.unipv.bitFactory.dao.sqlite.SqliteClienteDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteEventoDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteLegendsDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteMagazzinoDAO;
import it.unipv.bitFactory.dao.sqlite.SqlitePrenotazioneDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteSessioneDAO;

import it.unipv.bitFactory.model.magazzino.SoglieMagazzino;

import it.unipv.bitFactory.service.GestoreEventi;
import it.unipv.bitFactory.service.MagazzinoService;
import it.unipv.bitFactory.service.SistemaPrenotazioni;

import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) {

        try {

            /*
             * Percorso del database SQLite.
             */
            Path databasePath = Path.of(
                    "data",
                    "database_bfactory.db"
            ).toAbsolutePath().normalize();

            String percorsoDatabase =
                    databasePath.toString();

            /*
             * -------------------------------------------------
             * PARTE SESSIONI
             * -------------------------------------------------
             */

            /*
             * DAO usato per leggere e aggiornare
             * le macchine e i relativi pezzi.
             */
            LegendsDAO legendsDAO =
                    new SqliteLegendsDAO(
                            percorsoDatabase
                    );

            /*
             * DAO usato per inserire lo storico
             * delle sessioni nella tabella sessioni.
             */
            SessioneDAO sessioneDAO =
                    new SqliteSessioneDAO(
                            percorsoDatabase
                    );

            /*
             * Il controller riceve entrambi i DAO:
             *
             * - legendsDAO aggiorna macchina e pezzi;
             * - sessioneDAO registra la sessione.
             */
            GestioneSessioniController sessioniController =
                    new GestioneSessioniController(
                            legendsDAO,
                            sessioneDAO
                    );

            /*
             * -------------------------------------------------
             * PARTE MAGAZZINO
             * -------------------------------------------------
             */

            MagazzinoDAO magazzinoDAO =
                    new SqliteMagazzinoDAO(
                            percorsoDatabase,
                            new SoglieMagazzino(2)
                    );

            MagazzinoService magazzinoService =
                    new MagazzinoService(
                            magazzinoDAO
                    );

            GestioneMagazzinoController magazzinoController =
                    new GestioneMagazzinoController(
                            magazzinoService
                    );

            /*
             * -------------------------------------------------
             * PARTE EVENTI E PRENOTAZIONI
             * -------------------------------------------------
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

            GestoreEventi gestoreEventi =
                    new GestoreEventi(
                            eventoDAO
                    );


            SistemaPrenotazioni sistemaPrenotazioni =
                    new SistemaPrenotazioni(
                            clienteDAO,
                            eventoDAO,
                            prenotazioneDAO
                    );

            GestionePrenotazioniController
                    prenotazioniController =
                    new GestionePrenotazioniController(
                            sistemaPrenotazioni
                    );

            /*
             * -------------------------------------------------
             * AVVIO SERVER WEB
             * -------------------------------------------------
             */

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