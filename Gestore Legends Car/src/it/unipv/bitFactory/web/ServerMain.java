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
            ServizioSessioni sessioniService = new ServizioSessioni(legendsDAO, sessioneDAO);
            GestioneSessioniController sessioniController = new GestioneSessioniController(sessioniService);

            //magazzino
            MagazzinoDAO magazzinoDAO = new SqliteMagazzinoDAO(percorsoDatabase, new SoglieMagazzino(2));
            ServizioMagazzino magazzinoService = new ServizioMagazzino(magazzinoDAO, legendsDAO);
            GestioneMagazzinoController magazzinoController = new GestioneMagazzinoController(magazzinoService);

            //eventi e prenotazioni
            EventoDAO eventoDAO = new SqliteEventoDAO(percorsoDatabase);
            ClienteDAO clienteDAO = new SqliteClienteDAO(percorsoDatabase);
            PrenotazioneDAO prenotazioneDAO = new SqlitePrenotazioneDAO(percorsoDatabase);
            ServizioEventi gestoreEventi = new ServizioEventi(eventoDAO);
            GestioneEventiController eventiController = new GestioneEventiController(gestoreEventi);
            ServizioPrenotazioni sistemaPrenotazioni = new ServizioPrenotazioni(clienteDAO, eventoDAO,prenotazioneDAO);
            GestionePrenotazioniController prenotazioniController = new GestionePrenotazioniController(sistemaPrenotazioni);

            //autenticazione addetti
            AddettoDAO addettoDAO = new SqliteAddettoDAO(percorsoDatabase);
            ServizioAutenticazione servizioAutenticazione = new ServizioAutenticazione(addettoDAO);
            GestioneLoginController loginController = new GestioneLoginController(servizioAutenticazione);
            ServizioSessioniLogin gestoreSessioniLogin = new ServizioSessioniLogin();
            
            //parte server
            HtmlRenderer renderer = new HtmlRenderer();

            BitFactoryWebServer server = new BitFactoryWebServer(
            		
                            8082,
                            8,
                            sessioniController,
                            magazzinoController,
                            prenotazioniController,
                            loginController,
                            eventiController,
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