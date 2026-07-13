package it.unipv.bitFactory.controller;

import java.util.List;

import it.unipv.bitFactory.dao.SqliteMagazzinoDAO;
import it.unipv.bitFactory.dao.sqlite.SqliteClienteDAO;
import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.service.MagazzinoService;

public class GestioneMagazzinoController {

    private static final String PERCORSO_DATABASE_PREDEFINITO = "data/database_bitfactory.db";

    private final MagazzinoService magazzinoService;

    /**
     * Costruttore mantenuto per compatibilità con ServerMain.
     * Crea automaticamente DAO e service usando il database del progetto.
     */
    public GestioneMagazzinoController() {
        this(new MagazzinoService(
                new SqliteMagazzinoDAO(PERCORSO_DATABASE_PREDEFINITO)
        ));
    }

    public GestioneMagazzinoController(MagazzinoService magazzinoService) {
        if (magazzinoService == null) {
            throw new IllegalArgumentException("Il service magazzino non può essere null");
        }
        this.magazzinoService = magazzinoService;
    }

    public StatoDisponibilita controllaPezzo(String idPezzo) {
        return magazzinoService.controllaDisponibilita(idPezzo);
    }

    public VoceMagazzino cercaPezzo(String idPezzo) {
        return magazzinoService.cercaPezzo(idPezzo);
    }

    public List<VoceMagazzino> visualizzaMagazzino() {
        return magazzinoService.visualizzaMagazzino();
    }

    public List<VoceMagazzino> visualizzaPezziDisponibili() {
        return magazzinoService.visualizzaPezziDisponibili();
    }

    public void aggiungiPezzo(Pezzo pezzo) {
        magazzinoService.aggiungiPezzo(pezzo);
    }

    public void rimuoviPezzo(String idPezzo) {
        magazzinoService.rimuoviPezzo(idPezzo);
    }
}
