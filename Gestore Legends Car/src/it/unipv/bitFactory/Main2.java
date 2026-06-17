package it.unipv.bitFactory;

import java.io.IOException;

import it.unipv.bitFactory.web.WebServer;

public class Main2 {

    public static void main(String[] args) {
        try {
            WebServer webServer = new WebServer(8080);
            webServer.avvia();
        } catch (IOException e) {
            System.err.println("Impossibile avviare il server: "
                    + e.getMessage());
        }
    }
}