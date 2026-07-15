package it.unipv.bitFactory.web;

import java.nio.file.Path;

import it.unipv.bitFactory.controller.*;

import it.unipv.bitFactory.dao.interfacce.*;

import it.unipv.bitFactory.dao.sqlite.*;

import it.unipv.bitFactory.model.magazzino.SoglieMagazzino;

import it.unipv.bitFactory.service.*;

import it.unipv.bitFactory.web.view.HtmlRenderer;

public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) {

        try {

            Path databasePath = Path.of("data", "database_bfactory.db").toAbsolutePath().normalize();

            String percorsoDatabase = databasePath.toString();
            
            //sessioni
            LegendsDAO legendsDAO = new SqliteLegendsDAO(percorsoDatabase);
            SessioneDAO sessioneDAO = new SqliteSessioneDAO(percorsoDatabase);
            GestioneSessioniController sessioniController = new GestioneSessioniController(legendsDAO, sessioneDAO);

            //magazzino
            MagazzinoDAO magazzinoDAO = new SqliteMagazzinoDAO(
                    percorsoDatabase,
                    new SoglieMagazzino(2)
            );
            MagazzinoService magazzinoService = new MagazzinoService(
                    magazzinoDAO,
                    legendsDAO
            );
            GestioneMagazzinoController magazzinoController = new GestioneMagazzinoController(magazzinoService);

            //eventi e prenotazioni
            EventoDAO eventoDAO = new SqliteEventoDAO(percorsoDatabase);
            ClienteDAO clienteDAO = new SqliteClienteDAO(percorsoDatabase);
            PrenotazioneDAO prenotazioneDAO = new SqlitePrenotazioneDAO(percorsoDatabase);
            GestoreEventi gestoreEventi = new GestoreEventi(eventoDAO);
            SistemaPrenotazioni sistemaPrenotazioni = new SistemaPrenotazioni(clienteDAO, eventoDAO,prenotazioneDAO);
            GestionePrenotazioniController prenotazioniController = new GestionePrenotazioniController(sistemaPrenotazioni);

            //autenticazione addetti
            AddettoDAO addettoDAO = new SqliteAddettoDAO(percorsoDatabase);
            ServizioAutenticazione servizioAutenticazione = new ServizioAutenticazione(addettoDAO);
            LoginController loginController = new LoginController(servizioAutenticazione);
            GestoreSessioniLogin gestoreSessioniLogin = new GestoreSessioniLogin();
            
            //parte server
            HtmlRenderer renderer = new HtmlRenderer();

            BitFactoryWebServer server = new BitFactoryWebServer(
            		
                            8082,
                            8,
                            sessioniController,
                            magazzinoController,
                            prenotazioniController,
                            loginController,
                            gestoreEventi,
                            gestoreSessioniLogin,
                            renderer
                    );

            Runtime.getRuntime().addShutdownHook(new Thread(server::arresta));

            server.avvia();

        } catch (Exception e) {

            System.err.println("Errore durante l'avvio del server: "+ e.getMessage());

            e.printStackTrace();
        }
    }
}
