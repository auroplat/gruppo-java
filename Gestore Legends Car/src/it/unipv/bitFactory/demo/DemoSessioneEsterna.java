package it.unipv.bitFactory.demo;

import java.nio.file.Path;

import it.unipv.bitFactory.controller.GestioneSessioniController;
import it.unipv.bitFactory.dao.interfacce.LegendsDAO;
import it.unipv.bitFactory.dao.interfacce.SessioneDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteLegendsDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteSessioneDAO;
import it.unipv.bitFactory.external.SessioneEsterna;

public final class DemoSessioneEsterna {

    private DemoSessioneEsterna() {
    }

    public static void main(String[] args) {

        Path databasePath = Path.of(
                "data",
                "database_bfactory.db"
        ).toAbsolutePath().normalize();

        String percorsoDatabase =
                databasePath.toString();

        LegendsDAO legendsDAO =
                new SqliteLegendsDAO(
                        percorsoDatabase
                );

        SessioneDAO sessioneDAO =
                new SqliteSessioneDAO(
                        percorsoDatabase
                );

        GestioneSessioniController controller =
                new GestioneSessioniController(
                        legendsDAO,
                        sessioneDAO
                );

        SessioneEsterna sessioneEsterna =
                SessioneEsterna.gara(
                        "Silverstone",
                        52.4,
                        45,
                        2
                );

        controller.registraSessioneEsterna(
                "MAC001",
                sessioneEsterna
        );

        System.out.println(
                "Sessione esterna importata correttamente"
        );
    }
}