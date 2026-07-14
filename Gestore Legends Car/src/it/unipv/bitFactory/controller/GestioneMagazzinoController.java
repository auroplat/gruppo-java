package it.unipv.bitFactory.controller;

import java.util.List;

import it.unipv.bitFactory.model.magazzino.StatoDisponibilita;
import it.unipv.bitFactory.model.magazzino.VoceMagazzino;
import it.unipv.bitFactory.model.pezzi.Pezzo;
import it.unipv.bitFactory.model.pezzi.TipoPezzo;
import it.unipv.bitFactory.service.MagazzinoService;

public class GestioneMagazzinoController {

    private final MagazzinoService magazzinoService;

    public GestioneMagazzinoController(
            MagazzinoService magazzinoService) {

        if (magazzinoService == null) {
            throw new IllegalArgumentException(
                    "Il service magazzino non può essere null"
            );
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

    public StatoDisponibilita aggiornaQuantitaPezzo(
            String idPezzo,
            int nuovaQuantita) {

        return magazzinoService.aggiornaQuantitaPezzo(
                idPezzo,
                nuovaQuantita
        );
    }

    public List<Pezzo> trovaPezziLiberi(TipoPezzo tipoPezzo) {
        return magazzinoService.trovaPezziLiberi(tipoPezzo);
    }

    public void aggiungiPezzi(
            TipoPezzo tipoPezzo,
            int quantita,
            double kmMax,
            int tempoMax) {

        magazzinoService.aggiungiPezzi(
                tipoPezzo,
                quantita,
                kmMax,
                tempoMax
        );
    }

    public void creaMacchina(String idMacchina) {
        magazzinoService.creaMacchina(idMacchina);
    }

    public void cambiaPezzo(
            String idMacchina,
            TipoPezzo tipoPezzo,
            String idNuovoPezzo) {

        magazzinoService.cambiaPezzo(
                idMacchina,
                tipoPezzo,
                idNuovoPezzo
        );
    }
}
