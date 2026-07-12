package it.unipv.bitFactory.service;

import java.util.List;

import it.unipv.bitFactory.dao.MagazzinoDAO;
import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;

public class MagazzinoService {

    private final MagazzinoDAO magazzinoDAO;

    public MagazzinoService(MagazzinoDAO magazzinoDAO) {
        if (magazzinoDAO == null) {
            throw new IllegalArgumentException("Il DAO magazzino non può essere null");
        }

        this.magazzinoDAO = magazzinoDAO;
    }

    public StatoDisponibilita controllaDisponibilita(String idPezzo) {
        VoceMagazzino voce = magazzinoDAO.trovaPerIdPezzo(idPezzo)
                .orElseThrow(() -> new IllegalArgumentException("Pezzo non trovato: " + idPezzo));

        return voce.getStatoDisponibilita();
    }

    public VoceMagazzino cercaPezzo(String idPezzo) {
        return magazzinoDAO.trovaPerIdPezzo(idPezzo)
                .orElseThrow(() -> new IllegalArgumentException("Pezzo non trovato: " + idPezzo));
    }

    public List<VoceMagazzino> visualizzaMagazzino() {
        return magazzinoDAO.trovaTutti();
    }

    public StatoDisponibilita aggiornaQuantitaPezzo(String idPezzo, int nuovaQuantita) {
        magazzinoDAO.aggiornaQuantita(idPezzo, nuovaQuantita);

        return controllaDisponibilita(idPezzo);
    }
}